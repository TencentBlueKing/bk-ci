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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class StreamRepositoryPermissionServiceImpl @Autowired constructor(
    private val client: Client,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService,
    val repositoryDao: RepositoryDao
) : RepositoryPermissionService {
    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        repositoryId: Long?,
        message: String
    ) {
        val permissionCheck = hasPermission(userId, projectId, authPermission, repositoryId)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterRepository(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): List<Long> {
        if (hasPermission(userId, projectId, authPermission)) {
            return getAllRepository(projectId)
        }
        return emptyList()
    }

    override fun filterRepositories(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val checkPermission = hasPermission(userId, projectId, authPermissions.first())
        if (!checkPermission) {
            return emptyMap()
        }
        val allRepositoryInstances = getAllRepository(projectId)
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        authPermissions.forEach {
            resultMap[it] = allRepositoryInstances
        }
        return resultMap
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        repositoryId: Long?
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            projectCode = projectId,
            action = authPermission.value,
            token = tokenService.getSystemToken(null) ?: "",
            resourceCode = null
        ).data ?: false
    }

    override fun createResource(
        userId: String,
        projectId: String,
        repositoryId: Long,
        repositoryName: String
    ) {
        return
    }

    override fun editResource(
        projectId: String,
        repositoryId: Long,
        repositoryName: String
    ) {
        return
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        return
    }

    private fun getAllRepository(projectId: String): List<Long> {
        val repositoryInfos = repositoryDao.listByProject(
            dslContext = dslContext,
            projectId = projectId,
            repositoryType = null
        )
        return repositoryInfos.map { it.repositoryId }
    }
}
