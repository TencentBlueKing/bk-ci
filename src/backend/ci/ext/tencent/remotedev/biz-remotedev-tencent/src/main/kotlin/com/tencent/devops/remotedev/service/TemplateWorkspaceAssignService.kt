/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ConfigDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.TemplateWorkspaceAssignResp
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 按模版申请交付云桌面给用户。
 *
 * 通过 T_REMOTEDEV_CONFIG 以 JSON 数组维护"模版ID -> 多个交付项目"的映射，
 * 按序在各交付项目下查询"待分配"(DISTRIBUTING)实例，首个命中即分配拥有者给申请人。
 */
@Service
class TemplateWorkspaceAssignService @Autowired constructor(
    private val dslContext: DSLContext,
    private val configDao: ConfigDao,
    private val workspaceDao: WorkspaceDao,
    private val client: Client,
    private val deliverControl: DeliverControl,
    private val permissionService: PermissionService,
    private val tokenService: ClientTokenService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateWorkspaceAssignService::class.java)
        private const val CONFIG_KEY_PREFIX = "remotedev:"
        private const val ASSIGN_LOCK_KEY_PREFIX = "remotedev:template:assign:lock:"
        private const val ASSIGN_LOCK_EXPIRED_SECONDS = 30L
    }

    fun assignByTemplate(applicant: String, templateId: String): TemplateWorkspaceAssignResp {
        logger.info("assignByTemplate|applicant=$applicant|templateId=$templateId")
        val projectIds = resolveProjectIds(templateId)
        logger.info("assignByTemplate|templateId=$templateId|candidateProjects=$projectIds")

        // 按序遍历候选项目(数组顺序即优先级)，前面项目资源耗尽则回退到后续项目
        projectIds.forEach { projectId ->
            val resp = tryAssignInProject(applicant, templateId, projectId)
            if (resp != null) {
                return resp
            }
        }

        logger.warn("assignByTemplate|no available workspace|templateId=$templateId|projects=$projectIds")
        throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_AVAILABLE_DISTRIBUTING_WORKSPACE.errorCode,
            params = arrayOf(templateId)
        )
    }

    private fun resolveProjectIds(templateId: String): List<String> {
        val configValue = configDao.fetchConfig(dslContext, "$CONFIG_KEY_PREFIX$templateId")?.trim()
        if (configValue.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.TEMPLATE_PROJECT_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(templateId)
            )
        }
        // 优先按 JSON 数组解析；兼容历史单值配置
        val projectIds = if (configValue.startsWith("[")) {
            try {
                JsonUtil.to(configValue, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                logger.warn("assignByTemplate|parse config json fail|templateId=$templateId|value=$configValue", e)
                emptyList()
            }
        } else {
            listOf(configValue)
        }.map { it.trim() }.filter { it.isNotBlank() }

        if (projectIds.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.TEMPLATE_PROJECT_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(templateId)
            )
        }
        return projectIds
    }

    private fun tryAssignInProject(
        applicant: String,
        templateId: String,
        projectId: String
    ): TemplateWorkspaceAssignResp? {
        // 项目级分布式锁：串行化"选实例+改状态"，杜绝同一实例并发分配给多人
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "$ASSIGN_LOCK_KEY_PREFIX$projectId",
            expiredTimeInSeconds = ASSIGN_LOCK_EXPIRED_SECONDS
        )
        lock.use {
            lock.lock()
            // 1、添加用户到项目用户组(AnyDev云桌面用户组)：先判断用户是否有项目权限，
            // 没有才创建用户组 + 加人，有“AnyDev云桌面用户组”直接加人，没有才创建；
            if (!permissionService.checkUserVisitPermission(applicant, projectId)) {
                val gpId = client.get(ServiceResourceGroupResource::class).createCustomGroupAndPermissions(
                    projectCode = projectId,
                    customGroupCreateReq = CustomGroupCreateReq(
                        groupName = "AnyDev云桌面用户组",
                        groupDesc = "AnyDev云桌面用户组，用于控制AnyDev的权限",
                        actions = listOf("project_visit")
                    )
                ).data ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                    params = arrayOf("create AnyDev云桌面用户组 error no id")
                )
                val res = client.get(ServiceResourceMemberResource::class).batchAddResourceGroupMembers(
                    tokenService.getSystemToken(),
                    projectId,
                    ProjectCreateUserInfo(
                        createUserId = applicant,
                        groupId = gpId,
                        userIds = listOf(applicant),
                        roleName = null,
                        roleId = null,
                        deptIds = null,
                        resourceType = null,
                        resourceCode = null
                    )
                ).data
                if (res != true) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                        params = arrayOf("add ${applicant} AnyDev云桌面用户组 error")
                    )
                }
            }

            // 仅取 IP 已就绪的待分配实例；该项目无可用实例则返回 null，由上层回退到下一个候选项目
            val workspace = workspaceDao.fetchOldestDistributingWorkspace(dslContext, projectId)
            if (workspace == null) {
                logger.info("assignByTemplate|no distributing workspace|templateId=$templateId|projectId=$projectId")
                return null
            }
            // SQL 已过滤 IP 为空的记录，此处仅作防御，理论上不会为空
            val ip = workspace.ip ?: return null
            logger.info(
                "assignByTemplate|hit workspace|templateId=$templateId" +
                    "|projectId=$projectId|workspaceName=${workspace.workspaceName}"
            )
            // 复用交付逻辑：分配拥有者、状态 DISTRIBUTING->RUNNING、记录历史、同步权限
            deliverControl.assignUser2Workspace(
                userId = applicant,
                workspaceName = workspace.workspaceName,
                assigns = listOf(
                    ProjectWorkspaceAssign(
                        userId = applicant,
                        type = WorkspaceShared.AssignType.OWNER,
                        expiration = null
                    )
                ),
                checkPermission = false
            )
            return TemplateWorkspaceAssignResp(
                workspaceName = workspace.workspaceName,
                ip = ip
            )
        }
    }
}
