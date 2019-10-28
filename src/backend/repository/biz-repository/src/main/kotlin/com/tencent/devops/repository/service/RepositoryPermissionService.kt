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

package com.tencent.devops.repository.service

import com.tencent.devops.common.auth.api.AuthPermission

interface RepositoryPermissionService {
    fun validatePermission(
            userId: String,
            projectId: String,
            authPermission: AuthPermission,
            repositoryId: Long? = null,
            message: String
    )

    fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long>
    fun filterRepositories(
            userId: String,
            projectId: String,
            authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>>

    fun hasPermission(
            userId: String,
            projectId: String,
            authPermission: AuthPermission,
            repositoryId: Long? = null
    ): Boolean

    fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String)
    fun editResource(projectId: String, repositoryId: Long, repositoryName: String)
    fun deleteResource(projectId: String, repositoryId: Long)

    fun getUserResourcesByPermissions(
            user: String,
            projectCode: String,
            permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>>

    fun getUserResourceByPermission(
            user: String,
            projectCode: String,
            permission: AuthPermission
    ): List<String>

    fun validateUserResourcePermission(
            user: String,
            projectCode: String,
            permission: AuthPermission
    ): Boolean

    fun validateUserResourcePermission(
            user: String,
            projectCode: String,
            resourceCode: String,
            permission: AuthPermission
    ): Boolean

    fun modifyResource(
            projectCode: String,
            resourceCode: String,
            resourceName: String
    )
}