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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BK_DEVOPS_SCOPE

@Suppress("TooManyFunctions")
class MockAuthProjectApi constructor(
    private val bkAuthPermissionApi: MockAuthPermissionApi
) : AuthProjectApi {

    override fun validateUserProjectPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        return if (permission == AuthPermission.MANAGE || permission == AuthPermission.ENABLE) {
            checkProjectManager(userId = user, serviceCode = serviceCode, projectCode = projectCode)
        } else {
            checkProjectUser(user = user, serviceCode = serviceCode, projectCode = projectCode)
        }
    }

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return true
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return true
    }

    override fun checkProjectManager(userId: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return true
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
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = BK_DEVOPS_SCOPE,
            permissions = setOf(AuthPermission.MANAGE),
            supplier = supplier
        )
        val sets = mutableSetOf<String>()
        map.map { sets.addAll(it.value) }
        return sets.toList()
    }

    override fun getUserProjectsByPermission(
        serviceCode: AuthServiceCode,
        userId: String,
        permission: AuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        return emptyList()
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        val map = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = BK_DEVOPS_SCOPE,
            permissions = setOf(AuthPermission.VIEW, AuthPermission.MANAGE),
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

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        return true
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectInfo(serviceCode: AuthServiceCode, projectId: String): BkAuthProjectInfoResources? {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
