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

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectPermissionService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.beans.factory.annotation.Autowired

class TxV0ProjectPermissionServiceImpl @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val authProperties: BkAuthProperties,
    private val authProjectApi: BSAuthProjectApi,
    private val authTokenApi: AuthTokenApi,
    private val bsProjectAuthServiceCode: BSProjectServiceCodec,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val managerService: ManagerService
) : ProjectPermissionService {

    private val authUrl = authProperties.url

    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        resourceCreateInfo: AuthProjectCreateInfo
    ): String {
        val accessToken = resourceCreateInfo.accessToken
        val userId = resourceCreateInfo.userId
        val userDeptDetail = resourceCreateInfo.userDeptDetail
        val param: MutableMap<String, String> = mutableMapOf("project_code" to resourceRegisterInfo.resourceCode)
        // 创建AUTH项目
        val newAccessToken = if (accessToken.isNullOrBlank()) {
            param["creator"] = userId
            authTokenApi.getAccessToken(bsProjectAuthServiceCode)
        } else accessToken
        val authUrl = "$authUrl/projects?access_token=$newAccessToken"
        logger.info("create project $authUrl $userId,use userAccessToken${newAccessToken == accessToken}")
        if (userDeptDetail != null) {
            param["bg_id"] = userDeptDetail.bgId
            param["dept_id"] = userDeptDetail.deptId
            param["center_id"] = userDeptDetail.centerId
            logger.info("createProjectResources add org info $param")
        }
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = objectMapper.writeValueAsString(param)
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder().url(authUrl).post(requestBody).build()
        val responseContent = request(request, "调用权限中心创建项目失败")
        val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to create the project of response $responseContent")
            throw OperationException("调用权限中心创建项目失败: ${result.message}")
        }
        val authProjectForCreateResult = result.data
        return if (authProjectForCreateResult != null) {
            if (authProjectForCreateResult.project_id.isBlank()) {
                throw OperationException("权限中心创建的项目ID无效")
            }
            authProjectForCreateResult.project_id
        } else {
            logger.warn("Fail to get the project id from response $responseContent")
            throw OperationException("权限中心创建的项目ID无效")
        }
    }

    override fun deleteResource(projectCode: String) {
        // 内部版用不到
    }

    override fun modifyResource(
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        authResourceApi.modifyResource(
            serviceCode = bsProjectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = resourceUpdateInfo.projectUpdateInfo.englishName,
            resourceCode = resourceUpdateInfo.projectUpdateInfo.englishName,
            resourceName = resourceUpdateInfo.projectUpdateInfo.projectName
        )
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(bsProjectAuthServiceCode, userId, null)
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(bsProjectAuthServiceCode, userId, null)
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code} , message ${response.message} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        val accessTokenNew = if (accessToken.isNullOrEmpty()) {
            authTokenApi.getAccessToken(bsProjectAuthServiceCode)
        } else accessToken
        val url = "${authProperties.url}/projects/$projectCode/users/$userId/verfiy?access_token=$accessTokenNew"
        logger.info("the verifyUserProjectPermission url is:$url")
        val body = RequestBody.create(MessageProperties.CONTENT_TYPE_JSON.toMediaTypeOrNull(), "{}")
        val request = Request.Builder().url(url).post(body).build()
        val responseContent = request(request, "verifyUserProjectPermission error")
        val result = objectMapper.readValue<Result<Any?>>(responseContent)
        logger.info("the verifyUserProjectPermission result is:$result")
        if (result.isNotOk()) {
            return managerService.isManagerPermission(userId, projectCode, AuthResourceType.PROJECT, AuthPermission.VIEW)
        }
        return result.isOk()
    }

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String, permission: AuthPermission): Boolean {
        val isSuccess = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = bsProjectAuthServiceCode,
            projectCode = projectCode,
            permission = permission,
            resourceType = AuthResourceType.PROJECT
        )
        if (isSuccess) {
            return true
        }

        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectCode,
            resourceType = AuthResourceType.PROJECT,
            authPermission = permission
        )
    }

    override fun cancelCreateAuthProject(
        userId: String,
        projectCode: String
    ) = Unit

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) = Unit

    override fun needApproval(needApproval: Boolean?) = false

    companion object {
        val logger = LoggerFactory.getLogger(TxV0ProjectPermissionServiceImpl::class.java)
    }
}
