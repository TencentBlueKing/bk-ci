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

package com.tencent.devops.process.permission

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.project.api.service.ServiceProjectResource
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class V3PipelinePermissionService constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    authProjectApi: AuthProjectApi,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    pipelineAuthServiceCode: PipelineAuthServiceCode
) : AbstractPipelinePermissionService(
    authProjectApi = authProjectApi,
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    pipelineAuthServiceCode = pipelineAuthServiceCode
) {
    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkPipelinePermission(userId, projectId, pipelineId, permission)
    }

    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.isProjectUser(userId, projectId, group)
    }

    override fun checkPipelinePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        logger.info("checkPipelinePermission only check action project[$projectId]")
        if (isProjectOwner(projectId, userId)) {
            logger.info("project owner checkPipelinePermission success |$projectId|$userId")
            return true
        }
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            resourceCode = projectId,
            permission = AuthPermission.CREATE,
            relationResourceType = AuthResourceType.PROJECT
        )
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        logger.info("validPipelinePermission V3 impl projectId[$projectId] pipelineId[$pipelineId]")
        if (isProjectOwner(projectId, userId)) {
            logger.info("project owner valid success |$projectId|$userId")
            return
        }

        var authResourceType: AuthResourceType? = null
        if (pipelineId == "*") {
            authResourceType = AuthResourceType.PROJECT
        }
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = permission,
                relationResourceType = authResourceType
            )
        ) {
            val permissionMsg = permission.getI18n(I18nUtil.getLanguage(userId))
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun getResourceByPermission(userId: String, projectId: String, permission: AuthPermission): List<String> {
        val instances = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            super.getResourceByPermission(userId, projectId, permission)
        }
        if (instances.contains("*")) {
            val pipelineIds = mutableListOf<String>()
            val pipelineInfos = pipelineInfoDao.searchByProject(dslContext, projectId)
            pipelineInfos?.map {
                pipelineIds.add(it.pipelineId)
            }
            return pipelineIds
        }
        return instances
    }

    override fun filterPipelines(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        pipelineIds: List<String>
    ): Map<AuthPermission, List<String>> {
        return authPermissions.associateWith {
            pipelineIds
        }
    }

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("pipeline permission get ProjectOwner $projectId | $projectCreator| $userId")
            return if (!projectCreator.isNullOrEmpty()) {
                redisOperation.set(OwnerUtils.getOwnerRedisKey(projectId), projectCreator)
                userId == projectCreator
            } else {
                false
            }
        } else {
            return userId == cacheOwner
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V3PipelinePermissionService::class.java)
    }
}
