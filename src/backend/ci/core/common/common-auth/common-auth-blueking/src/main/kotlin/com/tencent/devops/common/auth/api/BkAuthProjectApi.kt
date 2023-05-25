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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BK_DEVOPS_SCOPE
import com.tencent.devops.common.auth.code.BkProjectAuthServiceCode
import com.tencent.devops.common.auth.code.GLOBAL_SCOPE_TYPE
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.BadRequestException

@Suppress("ALL")
class BkAuthProjectApi constructor(
    private val bkAuthPermissionApi: BkAuthPermissionApi,
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val projectAuthServiceCode: BkProjectAuthServiceCode,
    private val bkAuthTokenApi: BkAuthTokenApi
) : AuthProjectApi {

    override fun validateUserProjectPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        // v0没有project_enable权限,启用/禁用只有管理员才有权限
        return if (permission == AuthPermission.MANAGE || permission == AuthPermission.ENABLE) {
            checkProjectManager(userId = user, serviceCode = serviceCode, projectCode = projectCode)
        } else {
            checkProjectUser(user = user, serviceCode = serviceCode, projectCode = projectCode)
        }
    }

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
//        return emptyList()
        val accessToken = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = if (group == null) {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken"
        } else {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken&group_code=${group.value}"
        }
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get project users. $responseContent")
                throw RemoteServiceException("Fail to get project users", response.code, responseContent)
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<String>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == 403) {
                    bkAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to get project users. $responseContent")
                throw BadRequestException(responseObject.message)
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        if (group != null && group == BkAuthGroup.MANAGER) {
            return checkProjectManager(user, serviceCode, projectCode)
        }
        return checkProjectUser(user, serviceCode, projectCode)
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        val userResourcesByPermissions = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = projectCode,
            permissions = setOf(AuthPermission.VIEW)
        ) { emptyList() }

        return userResourcesByPermissions.isNotEmpty()
    }

    override fun checkProjectManager(
        userId: String,
        serviceCode: AuthServiceCode,
        projectCode: String
    ): Boolean {
        val userResourcesByPermissions = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = projectCode,
            permissions = setOf(AuthPermission.MANAGE)
        ) { emptyList() }

        return userResourcesByPermissions.isNotEmpty()
    }

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        TODO("not implemented")
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        TODO("not implemented")
    }

    override fun getProjectInfo(serviceCode: AuthServiceCode, projectId: String): BkAuthProjectInfoResources? {
        TODO("not implemented")
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
            resourceType = AuthResourceType.PROJECT,
            scopeId = BK_DEVOPS_SCOPE,
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
            userId = userId,
            scopeType = GLOBAL_SCOPE_TYPE,
            scopeId = BK_DEVOPS_SCOPE,
            resourceType = AuthResourceType.PROJECT,
            permissions = setOf(AuthPermission.VIEW, AuthPermission.MANAGE),
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

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkAuthProjectApi::class.java)
    }
}
