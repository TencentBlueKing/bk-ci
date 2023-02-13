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

package com.tencent.devops.repository.service.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.jooq.DSLContext

class RbacRepositoryPermissionService(
    private val repositoryDao: RepositoryDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val tokenService: ClientTokenService
) : RepositoryPermissionService {
    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?, message: String) {
        if (!hasPermission(userId, projectId, authPermission, repositoryId)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        val managerIds = mutableListOf<Long>()
        val resourceCodeList = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = AuthResourceType.CODE_REPERTORY.value,
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.CODE_REPERTORY)
        ).data ?: emptyList()

        if (resourceCodeList.isEmpty()) {
            return emptyList()
        }

        if (resourceCodeList.contains("*")) {
            repositoryDao.listByProject(dslContext, projectId, null)
                .map { managerIds.add(it.repositoryId.toLong()) }
            return managerIds
        }

        return resourceCodeList.map { it.toLong() }
    }

    override fun filterRepositories(userId: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val actions = mutableListOf<String>()
        authPermissions.forEach {
            actions.add(RbacAuthUtils.buildAction(it, AuthResourceType.CODE_REPERTORY))
        }

        val permissionResourcesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = actions,
            resourceType = AuthResourceType.CODE_REPERTORY.value
        ).data ?: emptyMap()

        val projectRepositoryIds = repositoryDao.listByProject(dslContext, projectId, null)
            .map { it.repositoryId }

        val resultMap = mutableMapOf<AuthPermission, List<Long>>()

        permissionResourcesMap.forEach { key, value ->
            val ids = if (value.contains("*")) {
                projectRepositoryIds
            } else {
                value.map { it.toLong() }
            }
            resultMap[key] = ids
            // todo 再确定一下，这里要去掉，因为rbac有  list权限，会拿 list动作，去获取资源，所以不会拉到 list动作的权限
            /*if (key == AuthPermission.VIEW) {
                resultMap[AuthPermission.LIST] = ids
            }*/
        }
        return resultMap
    }

    override fun hasPermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?): Boolean {
        val resourceCode: String
        val resourceType: String
        if (repositoryId != null) {
            resourceCode = repositoryId.toString()
            resourceType = AuthResourceType.CODE_REPERTORY.value
        } else {
            resourceCode = projectId
            resourceType = AuthResourceType.PROJECT.value
        }
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            relationResourceType = null,
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.CODE_REPERTORY),
            resourceCode = resourceCode
        ).data ?: false
    }

    override fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.CODE_REPERTORY.value,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.CODE_REPERTORY.value,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.CODE_REPERTORY.value,
            resourceCode = repositoryId.toString()
        )
    }
}
