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

package com.tencent.devops.common.auth.api.v3

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import org.springframework.beans.factory.annotation.Autowired

class TxV3AuthProjectApi @Autowired constructor(
    val client: Client,
    val tokenService: ClientTokenService
) : AuthProjectApi {
    override fun validateUserProjectPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        // v3没有project_enable权限,启用/禁用只有管理员才有权限
        return if (permission == AuthPermission.MANAGE || permission == AuthPermission.ENABLE) {
            checkProjectManager(userId = user, serviceCode = serviceCode, projectCode = projectCode)
        } else {
            checkProjectUser(user = user, serviceCode = serviceCode, projectCode = projectCode)
        }
    }

    override fun getProjectUsers(
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): List<String> {
        return client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectCode,
            group = group
        ).data ?: emptyList()
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        return client.get(ServiceProjectAuthResource::class).getProjectGroupAndUserList(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectCode
        ).data ?: emptyList()
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        return client.get(ServiceProjectAuthResource::class).getUserProjects(
            token = tokenService.getSystemToken(null)!!,
            userId = userId
        ).data ?: emptyList()
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        val projectList = getUserProjects(serviceCode, userId, supplier)
        val projectMap = mutableMapOf<String, String>()
        projectList.map {
            projectMap[it] = it
        }
        return projectMap
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectCode,
            group = group
        ).data ?: false
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectCode
        ).data ?: false
    }

    override fun checkProjectManager(userId: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return client.get(ServiceProjectAuthResource::class).checkProjectManager(
            userId = userId,
            projectCode = projectCode,
            token = tokenService.getSystemToken(null)!!
        ).data ?: false
    }

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).createProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectCode,
            roleCode = role
        ).data ?: false
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        return client.get(ServiceProjectAuthResource::class).getProjectRoles(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectCode,
            projectId = projectId
        ).data ?: emptyList()
    }

    override fun getProjectInfo(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): BkAuthProjectInfoResources? {
        return null
    }
}
