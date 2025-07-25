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
 */

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import io.fabric8.kubernetes.api.model.Service
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class ServiceClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceClient::class.java)
    }

    fun createService(
        userId: String,
        namespace: String,
        service: Service
    ): KubernetesResult<String> {
        val url = "/api/namespace/$namespace/services"
        val body = JsonUtil.toJson(service)
        logger.info("$userId Create service request url: $url, body: $body")
        val request = clientCommon.microBaseRequest(url).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body
            )
        ).build()
        val responseBody = OkhttpUtils.doHttp(request).body!!.string()
        logger.info("Create service response: ${JsonUtil.toJson(responseBody)}")
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getServiceByName(
        userId: String,
        namespace: String,
        serviceName: String
    ): KubernetesResult<Service> {
        val url = "/api/namespace/$namespace/services/$serviceName"
        val request = clientCommon.microBaseRequest(url).get().build()
        logger.info("Get service: $serviceName request url: $url, userId: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get service: $serviceName response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to get service,http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun deleteServiceByName(
        userId: String,
        namespace: String,
        serviceName: String
    ): KubernetesResult<String> {
        val url = "/api/namespace/$namespace/services/$serviceName"
        val request = clientCommon.microBaseRequest(url).delete().build()
        logger.info("Delete service: $serviceName request url: $url, userId: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Delete service: $serviceName response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to delete service,http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
