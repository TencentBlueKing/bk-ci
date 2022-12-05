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
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.iam.ProjectIamV0Service
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class V0ProjectExtPermissionServiceImpl @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val projectIamV0Service: ProjectIamV0Service,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
) : ProjectExtPermissionService {

    @Value("\${auth.url}")
    private lateinit var authUrl: String

    override fun verifyUserProjectPermission(accessToken: String, projectCode: String, userId: String): Boolean {
        val url = "$authUrl/$projectCode/users/$userId/verfiy?access_token=$accessToken"
        logger.info("the verifyUserProjectPermission url is:$url")
        val body = RequestBody.create(MediaType.parse(MessageProperties.CONTENT_TYPE_JSON), "{}")
        val request = Request.Builder().url(url).post(body).build()
        val responseContent = request(request, "verifyUserProjectPermission error")
        val result = objectMapper.readValue<Result<Any?>>(responseContent)
        logger.info("the verifyUserProjectPermission result is:$result")
        if (result.isOk()) {
            return true
        }
        return false
    }

    override fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?,
        checkManager: Boolean
    ): Boolean {
        return if (checkManager) {
            // 相对下面的实现，多做了管理员校验
            projectIamV0Service.createUser2Project(
                createUser = createUser,
                userIds = userIds,
                projectCode = projectCode,
                roleId = roleId,
                roleName = roleName
            )
        } else {
            projectIamV0Service.createUser2ProjectImpl(
                userIds = userIds,
                projectId = projectCode,
                roleName = roleName,
                roleId = roleId
            )
        }
    }

    override fun grantInstancePermission(
        userId: String,
        projectId: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        userList: List<String>
    ): Boolean {
        return projectIamV0Service.createPermission(
            userId = userId,
            projectId = projectId,
            permission = action,
            resourceType = resourceType,
            authServiceCode = bsPipelineAuthServiceCode,
            resourceTypeCode = resourceCode,
            userList = userList
        )
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} ," +
                                " message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(V0ProjectExtPermissionServiceImpl::class.java)
    }
}
