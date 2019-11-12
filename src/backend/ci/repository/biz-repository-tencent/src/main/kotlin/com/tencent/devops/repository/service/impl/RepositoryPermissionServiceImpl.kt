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

import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.code.BSCodeAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPermissionServiceImpl @Autowired constructor(
        private val authResourceApi: AuthResourceApi,
        private val authPermissionApi: AuthPermissionApi,
        private val codeAuthServiceCode: BSCodeAuthServiceCode
) : RepositoryPermissionService {

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?, message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filterRepositories(userId: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPermission(userId: String, projectId: String, authPermission: AuthPermission, repositoryId: Long?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.createResource(
                userId,
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString(),
                repositoryName
        )
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.modifyResource(
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString(),
                repositoryName
        )
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        authResourceApi.deleteResource(
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectId,
                repositoryId.toString()
        )
    }

    override fun getUserResourcesByPermissions(user: String, projectCode: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
                user = user,
                serviceCode = codeAuthServiceCode,
                resourceType = AuthResourceType.CODE_REPERTORY,
                projectCode = projectCode,
                permissions = permissions,
                supplier = null
        )
        return permissionResourcesMap.mapValues {
            it.value.map { it.toString() }
        }
    }

    override fun getUserResourceByPermission(user: String, projectCode: String, permission: AuthPermission): List<String> {
        val resourceCodeList = authPermissionApi.getUserResourceByPermission(
                user,
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                permission,
                null
        )
        return resourceCodeList.map { it }
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
                user,
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                permission
        )
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, resourceCode: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
                user,
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                resourceCode,
                permission
        )
    }

    override fun modifyResource(projectCode: String, resourceCode: String, resourceName: String) {
        authResourceApi.modifyResource(
                codeAuthServiceCode,
                AuthResourceType.CODE_REPERTORY,
                projectCode,
                resourceCode,
                resourceName
        )
    }
}