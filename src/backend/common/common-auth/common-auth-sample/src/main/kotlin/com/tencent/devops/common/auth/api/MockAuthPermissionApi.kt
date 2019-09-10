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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.code.AuthServiceCode

class MockAuthPermissionApi : AuthPermissionApi {

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission
    ): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: BkAuthPermission
    ): Boolean {
        return true
    }

    override fun getUserResourceByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        return supplier?.invoke() ?: emptyList()
    }

    override fun getUserResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode, // 对应新版的systemId
        resourceType: BkAuthResourceType,
        projectCode: String,
        permissions: Set<BkAuthPermission>,
        supplier: (() -> List<String>)?
    ): Map<BkAuthPermission, List<String>> {
        return getUserResourcesByPermissions(
            userId = user,
            scopeType = "Project",
            scopeId = projectCode,
            resourceType = resourceType,
            permissions = permissions,
            systemId = serviceCode,
            supplier = supplier
        )
    }

    override fun getUserResourcesByPermissions(
        userId: String,
        scopeType: String,
        scopeId: String,
        resourceType: BkAuthResourceType,
        permissions: Set<BkAuthPermission>,
        systemId: AuthServiceCode,
        supplier: (() -> List<String>)?
    ): Map<BkAuthPermission, List<String>> {

        val list = supplier?.invoke() ?: emptyList()
        val mock = mutableMapOf<BkAuthPermission, List<String>>()
        permissions.forEach { permission ->
            mock[permission] = list
        }
        return mock
    }
}