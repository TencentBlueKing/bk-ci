/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkauth

import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * 对接devops权限
 */
class BkAuthPermissionServiceImpl constructor(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    projectClient: ProjectClient,
    private val bkAuthConfig: BkAuthConfig,
    private val bkAuthPipelineService: BkAuthPipelineService,
    private val bkAuthProjectService: BkAuthProjectService
) : PermissionServiceImpl(
    userRepository,
    roleRepository,
    permissionRepository,
    mongoTemplate,
    repositoryClient,
    projectClient
) {
    private fun parsePipelineId(path: String): String? {
        val roads = PathUtils.normalizeFullPath(path).split("/")
        return if (roads.size < 2 || roads[1].isBlank()) {
            logger.warn("parse pipelineId failed, path: $path")
            null
        } else {
            roads[1]
        }
    }

    private fun checkDevopsPermission(request: CheckPermissionRequest): Boolean {
        with(request) {
            logger.debug("check devops permission request [$request]")

            // project权限
            if (resourceType == ResourceType.PROJECT.toString()) {
                // devops直接放过
                if (appId == bkAuthConfig.devopsAppId) return true
                // 其它请求校验项目权限
                return checkProjectPermission(uid, projectId!!, action)
            }

            // repo或者node权限
            val pass = when (repoName) {
                CUSTOM, LOG -> {
                    checkProjectPermission(uid, projectId!!, action)
                }
                PIPELINE -> {
                    checkPipelineOrProjectPermission(request)
                }
                REPORT -> {
                    checkReportPermission(action)
                }
                else -> {
                    super.checkPermission(request) || checkProjectPermission(uid, projectId!!, action)
                }
            }

            // devops来源的账号，不做拦截
            if (!pass && appId == bkAuthConfig.devopsAppId) {
                logger.warn("devops forbidden [$request]")
            }

            logger.debug("devops pass [$request]")
            return pass
        }
    }

    private fun checkPipelineOrProjectPermission(request: CheckPermissionRequest): Boolean {
        with(request) {
            var projectPass = false
            val pipelinePass = checkPipelinePermission(uid, projectId!!, path, resourceType, action)
            if (!pipelinePass) {
                logger.warn("devops pipeline permission check fail [$request]")
                projectPass = checkProjectPermission(uid, projectId!!, action)
                if (projectPass) logger.warn("devops pipeline permission widen to project permission [$request]")
            }
            return pipelinePass || projectPass
        }
    }

    private fun checkReportPermission(action: String): Boolean {
        return action == PermissionAction.READ.toString() ||
            action == PermissionAction.WRITE.toString() ||
            action == PermissionAction.VIEW.toString()
    }

    private fun checkPipelinePermission(
        uid: String,
        projectId: String,
        path: String?,
        resourceType: String,
        action: String
    ): Boolean {
        return when (resourceType) {
            ResourceType.REPO.toString() -> checkProjectPermission(uid, projectId, action)
            ResourceType.NODE.toString() -> {
                val pipelineId = parsePipelineId(path ?: return false) ?: return false
                pipelinePermission(uid, projectId, pipelineId, action)
            }
            else -> throw RuntimeException("resource type not supported: $resourceType")
        }
    }

    private fun pipelinePermission(uid: String, projectId: String, pipelineId: String, action: String): Boolean {
        logger.debug("pipelinePermission, uid: $uid, projectId: $projectId, pipelineId: $pipelineId, action: $action")
        return bkAuthPipelineService.hasPermission(uid, projectId, pipelineId, action)
    }

    private fun checkProjectPermission(uid: String, projectId: String, action: String): Boolean {
        logger.debug("checkProjectPermission: uid: $uid, projectId: $projectId, action: $action")
        return when (action) {
            PermissionAction.MANAGE.toString() -> bkAuthProjectService.isProjectManager(uid, projectId)
            else -> bkAuthProjectService.isProjectMember(uid, projectId, action)
        }
    }

    override fun listPermissionRepo(projectId: String, userId: String, appId: String?): List<String> {
        appId?.let {
            val request = buildProjectCheckRequest(projectId, userId, appId)

            // devops 体系
            if (matchDevopsCond(appId)) {
                if (checkDevopsPermission(request)) {
                    return getAllRepoByProjectId(projectId)
                }
                return emptyList()
            }
        }
        return super.listPermissionRepo(projectId, userId, appId)
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {

        // bcs或bkrepo账号
        if (matchBcsOrRepoCond(request.appId)) return super.checkPermission(request) || checkDevopsPermission(request)

        // devops账号
        if (matchDevopsCond(request.appId)) return checkDevopsPermission(request)

        // 非devops体系
        return super.checkPermission(request) || checkDevopsPermission(request)
    }

    private fun buildProjectCheckRequest(projectId: String, userId: String, appId: String): CheckPermissionRequest {
        return CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.PROJECT.toString(),
            action = PermissionAction.READ.toString(),
            projectId = projectId,
            appId = appId
        )
    }

    private fun matchBcsOrRepoCond(projectId: String?): Boolean {
        return projectId == bkAuthConfig.bcsAppId || projectId == bkAuthConfig.bkrepoAppId
    }

    private fun matchDevopsCond(appId: String?): Boolean {
        val devopsAppIdList = bkAuthConfig.devopsAppIdSet.split(",")
        return devopsAppIdList.contains(appId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPermissionServiceImpl::class.java)
        private const val CUSTOM = "custom"
        private const val PIPELINE = "pipeline"
        private const val REPORT = "report"
        private const val LOG = "log"
    }
}
