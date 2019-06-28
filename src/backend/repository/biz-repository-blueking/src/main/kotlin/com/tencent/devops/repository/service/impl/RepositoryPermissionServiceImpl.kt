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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPermissionServiceImpl @Autowired constructor(
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val codeAuthServiceCode: CodeAuthServiceCode
) : RepositoryPermissionService {

    override fun validatePermission(
        userId: String,
        projectId: String,
        bkAuthPermission: BkAuthPermission,
        repositoryId: Long?,
        message: String
    ) {
        if (!hasPermission(userId, projectId, bkAuthPermission, repositoryId)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterRepository(userId: String, projectId: String, bkAuthPermission: BkAuthPermission): List<Long> {
        val resourceCodeList = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = BkAuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permission = bkAuthPermission,
            supplier = null
        )
        return resourceCodeList.map { it.toLong() }
    }

    override fun filterRepositories(
        userId: String,
        projectId: String,
        bkAuthPermissions: Set<BkAuthPermission>
    ): Map<BkAuthPermission, List<Long>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = BkAuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permissions = bkAuthPermissions,
            supplier = null
        )
        return permissionResourcesMap.mapValues {
            it.value.map { id -> id.toLong() }
        }
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        bkAuthPermission: BkAuthPermission,
        repositoryId: Long?
    ): Boolean {
        if (repositoryId == null)
            return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = codeAuthServiceCode,
                resourceType = BkAuthResourceType.CODE_REPERTORY,
                projectCode = projectId,
                permission = bkAuthPermission
            )
        else
            return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = codeAuthServiceCode,
                resourceType = BkAuthResourceType.CODE_REPERTORY,
                projectCode = projectId,
                resourceCode = repositoryId.toString(),
                permission = bkAuthPermission
            )
    }

    override fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = BkAuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.modifyResource(
            serviceCode = codeAuthServiceCode,
            resourceType = BkAuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        authResourceApi.deleteResource(
            serviceCode = codeAuthServiceCode,
            resourceType = BkAuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString()
        )
    }
}
