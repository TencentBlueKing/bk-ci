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
 *
 */

package com.tencent.devops.process.permission.`var`

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions

class MockPublicVarGroupPermissionService : PublicVarGroupPermissionService {
    override fun checkPublicVarGroupPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean = true

    override fun checkPublicVarGroupPermissionWithMessage(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean = true

    override fun checkPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean = true

    override fun getPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        groupName: String
    ): PublicVarGroupPermissions {
        return PublicVarGroupPermissions(
            canEdit = true,
            canView = true,
            canDelete = true,
            canUse = true
        )
    }

    override fun filterPublicVarGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        groupNames: List<String>
    ): Map<AuthPermission, List<String>> {
        return authPermissions.associateWith { groupNames }
    }

    override fun createResource(
        userId: String,
        projectId: String,
        groupCode: String,
        name: String
    ) = Unit

    override fun deleteResource(
        projectId: String,
        groupName: String
    ) = Unit
}