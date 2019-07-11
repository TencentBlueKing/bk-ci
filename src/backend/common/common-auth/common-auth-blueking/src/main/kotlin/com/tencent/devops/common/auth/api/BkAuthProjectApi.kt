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

import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BK_DEVOPS_SCOPE
import com.tencent.devops.common.auth.code.BkProjectAuthServiceCode
import com.tencent.devops.common.auth.code.GLOBAL_SCOPE_TYPE

class BkAuthProjectApi constructor(
    private val bkAuthPermissionApi: BkAuthPermissionApi,
    private val projectAuthServiceCode: BkProjectAuthServiceCode
) : AuthProjectApi {

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {

        val authPermission = when (group) {
            BkAuthGroup.MANAGER -> BkAuthPermission.MANAGE
            else -> BkAuthPermission.VIEW
        }
        val userResourcesByPermissions = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = projectAuthServiceCode,
            resourceType = BkAuthResourceType.PROJECT,
            projectCode = projectCode,
            permissions = setOf(authPermission)
        ) { emptyList() }

        return userResourcesByPermissions.isNotEmpty()
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        val map = bkAuthPermissionApi.getUserResourcesByPermissions(
            userId = userId,
            scopeType = GLOBAL_SCOPE_TYPE,
            systemId = serviceCode,
            resourceType = BkAuthResourceType.PROJECT,
            scopeId = BK_DEVOPS_SCOPE,
            permissions = setOf(BkAuthPermission.MANAGE),
            supplier = supplier
        )
        val sets = mutableSetOf<String>()
        map.map { sets.addAll(it.value) }
        return sets.toList()
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        val map = bkAuthPermissionApi.getUserResourcesByPermissions(
            userId = userId,
            scopeType = GLOBAL_SCOPE_TYPE,
            scopeId = BK_DEVOPS_SCOPE,
            resourceType = BkAuthResourceType.PROJECT,
            permissions = setOf(BkAuthPermission.VIEW, BkAuthPermission.MANAGE),
            systemId = serviceCode,
            supplier = supplier
        )
        val sets = mutableSetOf<String>()
        map.values.forEach { l ->
            sets.addAll(l)
        }
        // 此处为兼容接口实现，并没有projectName,统一都是projectCode
        val projectCode2Code = mutableMapOf<String, String>()
        sets.forEach {
            projectCode2Code[it] = it
        }
        return projectCode2Code
    }
}