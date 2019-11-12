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

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectPermissionService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectPermissionServiceImpl @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val authProperties: BkAuthProperties,
    private val authProjectApi: BSAuthProjectApi,
    private val bsProjectAuthServiceCode: BSProjectServiceCodec
) : ProjectPermissionService {

    override fun createResources(userId: String, projectList: List<ResourceRegisterInfo>) {
        //内部版用不到
    }

    override fun deleteResource(projectCode: String) {
        //内部版用不到
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        //内部版用不到
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(bsProjectAuthServiceCode, userId, null)
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(bsProjectAuthServiceCode, userId, null)
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        val url = "${authProperties.url}/$projectCode/users/$userId/verfiy?access_token=$accessToken"
        logger.info("the verifyUserProjectPermission url is:$url")
        val body = RequestBody.create(MediaType.parse(MessageProperties.CONTENT_TYPE_JSON), "{}")
        val request = Request.Builder().url(url).post(body).build()
        val responseContent = request(request, "verifyUserProjectPermission error")
        val result = objectMapper.readValue<Result<Any?>>(responseContent)
        logger.info("the verifyUserProjectPermission result is:$result")
        return result.isOk()
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
