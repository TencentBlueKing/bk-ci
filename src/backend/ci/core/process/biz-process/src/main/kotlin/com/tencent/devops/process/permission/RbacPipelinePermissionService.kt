/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.view.PipelineViewGroupCommonService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class RbacPipelinePermissionService(
    val authPermissionApi: AuthPermissionApi,
    val authProjectApi: AuthProjectApi,
    val pipelineAuthServiceCode: PipelineAuthServiceCode,
    val dslContext: DSLContext,
    val pipelineInfoDao: PipelineInfoDao,
    val pipelineViewGroupCommonService: PipelineViewGroupCommonService,
    val authResourceApi: AuthResourceApi,
    val client: Client,
    val projectCacheService: ProjectCacheService
) : PipelinePermissionService {

    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        authResourceType: AuthResourceType?
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = authResourceType ?: resourceType,
            permission = permission,
            projectCode = projectId
        )
    }

    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        authResourceType: AuthResourceType?
    ): Boolean {
        logger.info("[rbac] check pipeline permission|$userId|$projectId|$pipelineId|$permission|$authResourceType")
        val startEpoch = System.currentTimeMillis()
        try {
            val pipelineInstance = pipeline2AuthResource(projectId, pipelineId, authResourceType)
            return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                projectCode = projectId,
                permission = permission,
                resource = pipelineInstance
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to check pipeline permission|" +
                    "$userId|$projectId|$pipelineId|$permission|$authResourceType"
            )
        }
    }

    private fun pipeline2AuthResource(
        projectId: String,
        pipelineId: String,
        authResourceType: AuthResourceType?
    ): AuthResourceInstance {
        val parents = mutableListOf<AuthResourceInstance>()
        val projectInstance = AuthResourceInstance(
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        parents.add(projectInstance)
        pipelineViewGroupCommonService.listViewIdsByPipelineId(projectId, pipelineId).forEach { viewId ->
            parents.add(
                AuthResourceInstance(
                    resourceType = AuthResourceType.PIPELINE_GROUP.value,
                    resourceCode = HashUtil.encodeLongId(viewId),
                    parents = listOf(projectInstance)
                )
            )
        }
        return AuthResourceInstance(
            resourceType = authResourceType?.value ?: resourceType.value,
            resourceCode = pipelineId,
            parents = parents
        )
    }

    private fun pipelines2AuthResources(
        projectId: String,
        pipelineIds: List<String>
    ): List<AuthResourceInstance> {
        val listViewIdsMap = pipelineViewGroupCommonService.listViewIdsMap(
            projectId = projectId,
            pipelineIds = pipelineIds
        )
        return pipelineIds.map { pipelineId ->
            val parents = mutableListOf<AuthResourceInstance>()
            val projectInstance = AuthResourceInstance(
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectId
            )
            parents.add(projectInstance)
            listViewIdsMap[pipelineId]?.forEach { viewId ->
                val pipelineGroupInstance = AuthResourceInstance(
                    resourceType = AuthResourceType.PIPELINE_GROUP.value,
                    resourceCode = HashUtil.encodeLongId(viewId),
                    parents = listOf(projectInstance)
                )
                parents.add(pipelineGroupInstance)
            }
            AuthResourceInstance(
                resourceType = resourceType.value,
                resourceCode = pipelineId,
                parents = parents
            )
        }
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        if (pipelineId == "*") {
            if (!checkPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    permission = permission
                )
            ) {
                throw PermissionForbiddenException(message)
            }
            return
        }

        val permissionCheck = checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun getResourceByPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): List<String> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        )
    }

    override fun filterPipelines(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        pipelineIds: List<String>
    ): Map<AuthPermission, List<String>> {
        logger.info("[rbac] filter pipeline|$userId|$projectId|$authPermissions")
        val startEpoch = System.currentTimeMillis()
        try {
            val resources = pipelines2AuthResources(projectId = projectId, pipelineIds = pipelineIds)
            return authPermissionApi.filterResourcesByPermissions(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                permissions = authPermissions,
                resources = resources
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to filter pipeline|" +
                    "$userId|$projectId|$authPermissions"
            )
        }
    }

    private fun getAllAuthPipelineIds(projectId: String): List<String> {
        return pipelineInfoDao.searchByProject(
            dslContext = dslContext, projectId = projectId
        )?.map { it.pipelineId }?.toList() ?: emptyList()
    }

    override fun createResource(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
        return authResourceApi.createResource(
            user = userId,
            projectCode = projectId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            resourceCode = pipelineId,
            resourceName = pipelineName
        )
    }

    override fun modifyResource(projectId: String, pipelineId: String, pipelineName: String) {
        authResourceApi.modifyResource(
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = pipelineId,
            resourceName = pipelineName
        )
    }

    override fun deleteResource(projectId: String, pipelineId: String) {
        authResourceApi.deleteResource(
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = pipelineId
        )
    }

    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectId,
            group = group,
            serviceCode = pipelineAuthServiceCode
        )
    }

    override fun checkProjectManager(userId: String, projectId: String): Boolean {
        return authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)
    }

    override fun isControlPipelineListPermission(projectId: String): Boolean {
        val projectInfo = projectCacheService.getProject(projectId) ?: return false
        return projectInfo.properties?.pipelineListPermissionControl == true
    }

    companion object {
        private val resourceType = AuthResourceType.PIPELINE_DEFAULT
        private val logger = LoggerFactory.getLogger(RbacPipelinePermissionService::class.java)
    }
}
