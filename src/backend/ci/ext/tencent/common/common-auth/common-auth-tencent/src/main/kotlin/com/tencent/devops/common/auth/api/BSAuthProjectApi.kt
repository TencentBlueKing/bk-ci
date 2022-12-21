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
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectCodeAndId
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.code.AuthServiceCode
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BSAuthProjectApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val bsCCProjectApi: BSCCProjectApi
) : AuthProjectApi {
    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return if (group != null) {
            getProjectUsers(serviceCode, projectCode, group).contains(user)
        } else {
            isProjectMember(user, projectCode, serviceCode)
        }
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return isProjectMember(user, projectCode, serviceCode)
    }

    override fun checkProjectManager(userId: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return getProjectUsers(serviceCode, projectCode, BkAuthGroup.MANAGER).contains(userId)
    }

    private fun isProjectMember(user: String, projectCode: String, serviceCode: AuthServiceCode): Boolean {
        logger.info("isProjectMember, user: $user, projectCode: $projectCode, serviceCode: $serviceCode")
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode/users/$user/verfiy?access_token=$accessToken"
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), "")
        val request = Request.Builder().url(url).post(body).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error("verify project user failed, url:$url, responseContent: $responseContent")
                throw RemoteServiceException("verify project user failed")
            }
            val responseObject = objectMapper.readValue<BkAuthResponse<Any>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                if (responseObject.code == HTTP_400) {
                    logger.info("user[$user] not member of project $projectCode")
                }
                logger.warn("verify project user failed. $responseContent")
//                throw RemoteServiceException("verify project user failed")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
                return false
            }
            return true
        }
    }

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        if (isAuthIgnore(projectCode)) {
            return emptyList()
        }
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        logger.info("getProjectUser accessToken:$accessToken")
        val url = if (group == null) {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken"
        } else {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken&group_code=${group.value}"
        }
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to get project users, url:$url,  $responseContent")
                throw RemoteServiceException("Fail to get project users")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<String>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.warn("Fail to get project users. $responseContent")
//                throw RemoteServiceException("Fail to get project users")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        if (isAuthIgnore(projectCode)) {
            return emptyList()
        }
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode/roles/?access_token=$accessToken&fields=user_list"
        val request = Request.Builder().url(url).get().build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to get project group and user list. $responseContent")
                throw RemoteServiceException("Fail to get project group and user list")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthGroupAndUserList>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.warn("Fail to get project group and user list. $responseContent")
                throw RemoteServiceException("Fail to get project group and user list")
                // #2836 只有当权限中心出现500系统，才抛出异常
//                if (responseObject.code >= HTTP_500) {
//                    throw RemoteServiceException(
//                        httpStatus = responseObject.code, errorMessage = responseObject.message
//                    )
//                }
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val tokenEscape = System.currentTimeMillis() - startEpoch
        val url = "${bkAuthProperties.url}/projects?access_token=$accessToken&user_id=$userId"
        val request = Request.Builder().url(url).get().build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            val escape = System.currentTimeMillis() - startEpoch
            if (escape > SLOW_TIME) {
                // if > 1 second, print the response
                logger.warn("[$userId|$serviceCode|$tokenEscape] It took ${escape}ms with response($responseContent)")
            }
            if (!response.isSuccessful) {
                logger.warn("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthProjectCodeAndId>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.warn("Fail to get user projects. $responseContent")
//                throw RemoteServiceException("Fail to get user projects")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
            }
            val projectCodeAndIdList = responseObject.data ?: emptyList()
            return projectCodeAndIdList.map { it.projectCode }
        }
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects?access_token=$accessToken&user_id=$userId"
        val request = Request.Builder().url(url).get().build()
//
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthProjectCodeAndId>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.warn("Fail to get user projects. $responseContent")
//                throw RemoteServiceException("Fail to get user projects")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
                return emptyMap()
            }
            val projectCodeAndIdList = responseObject.data ?: emptyList()
            val projecrCodeList = projectCodeAndIdList.map { it.projectCode }.toSet()
            val projectAvailableList = mutableMapOf<String, String>()
            val bkProjectInfos =
                bsCCProjectApi.getProjectListAsOuter(projecrCodeList).map { it.projectCode to it }.toMap()
            projectCodeAndIdList.forEach {
                val projectInfo = bkProjectInfos[it.projectCode] ?: return@forEach

                // 只有审批通过的项目才返回
                if (projectInfo.approvalStatus != "2" && projectInfo.approvalStatus != "1") {
                    return@forEach
                }
                // 只有未禁用的项目才返回
                if (projectInfo.isOfflined) {
                    return@forEach
                }
                projectAvailableList[it.projectCode] = projectInfo.projectName
            }
            return projectAvailableList
        }
    }

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        var result = false
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode/roles/$role/users?access_token=$accessToken"
        val bodyJson = mutableMapOf<String, String>()
        bodyJson["user_type"] = "rtx"
        bodyJson["user_id"] = user
        val content = objectMapper.writeValueAsString(bodyJson)
        logger.info("createProjectUser: url[$url], body:[$content]")
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url).post(body).build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("create project user fail: user[$user], projectCode[$projectCode]")
                throw RemoteServiceException("create project user fail: user[$user], projectCode[$projectCode]")
            }
            val responseObject = objectMapper.readValue<BkAuthResponse<Any>>(responseContent)
            result = if (responseObject.code != 0) {
                logger.warn("create project user fail: $responseObject")
                //                throw RemoteServiceException("create project user fail: $responseObject")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
                false
            } else {
                true
            }
        }
        return result
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        if (isAuthIgnore(projectCode)) {
            return emptyList()
        }
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode/roles?access_token=$accessToken"
        logger.info("getProjectRoles: url:$url")
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("get project roles fail: projectCode[$projectCode]")
                throw RemoteServiceException("get project roles fail: projectCode[$projectCode]")
            }

            val responseObject =
                objectMapper.readValue<BkAuthResponse<List<BKAuthProjectRolesResources>>>(responseContent)
            if (responseObject.code != 0) {
                logger.warn("get project role fail: $responseObject")
//                throw RemoteServiceException("get project role fail: $responseObject")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun getProjectInfo(serviceCode: AuthServiceCode, projectCode: String): BkAuthProjectInfoResources? {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode?access_token=$accessToken"
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("get project info fail: projectCode[$projectCode]")
                throw RemoteServiceException("get project inProjectPaasCCServicefo fail: projectCode[$projectCode]")
            }
            val responseObject = objectMapper.readValue<BkAuthResponse<BkAuthProjectInfoResources>>(responseContent)
            if (responseObject.code != 0) {
                logger.warn("get project info fail: $responseContent")
            }
            return responseObject.data
        }
    }

    private fun isAuthIgnore(projectCode: String): Boolean {
        val ignoreService = bkAuthProperties.ignoreService ?: return false
        val ignoreList = ignoreService.split(",")
        ignoreList?.forEach {
            if (projectCode.contains(it)) {
                logger.info("projectCode:$projectCode, ignore auth call")
                return true
            }
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BSAuthProjectApi::class.java)
        private const val HTTP_403 = 403
        private const val HTTP_400 = 400
        private const val HTTP_500 = 500
        private const val SLOW_TIME = 1000
    }
}
