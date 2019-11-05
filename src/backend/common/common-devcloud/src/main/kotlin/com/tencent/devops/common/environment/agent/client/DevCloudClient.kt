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

package com.tencent.devops.common.environment.agent.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.pojo.devcloud.Action
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainer
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImage
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DevCloudClient {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudClient::class.java)
    }

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""

    @Value("\${devCloud.smartProxyToken}")
    val smartProxyToken: String = ""

    fun createContainer(staffName: String, devCloudContainer: DevCloudContainer): String {
        val url = "$devCloudUrl/api/v2.1/containers"
        val body = ObjectMapper().writeValueAsString(devCloudContainer)
        logger.info("request body: $body")
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(addHeaders(staffName))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[$staffName] Fail to create container - [$devCloudContainer] with response [${response.code()}|${response.message()}|$responseContent]")
                throw RuntimeException("Fail to request to devCloud")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<*, *>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun operateContainer(staffName: String, name: String, action: Action): String {
        val url = "$devCloudUrl/api/v2.1/containers/$name"
        val body = when (action) {
            Action.DELETE -> "{\"action\":\"delete\",\"params\":{}}"
            Action.STOP -> "{\"action\":\"stop\",\"params\":{}}"
            Action.START -> "{\"action\":\"start\",\"params\":{}}"
            else -> ""
        }
        logger.info("request body: $body")
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(addHeaders(staffName))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[$staffName|$name|$action] Fail to get the request from url: $url with response: [${response.code()}|${response.message()}|$responseContent]")
                throw RuntimeException("Fail to start docker")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<*, *>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    private fun addHeaders(staffName: String) =
        Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken))

    fun getContainerInstance(staffName: String, id: String) =
        DevCloudContainerInstanceClient.getContainerInstance(
            devCloudUrl,
            devCloudAppId,
            devCloudToken,
            staffName,
            id,
            smartProxyToken
        )

    fun createImage(staffName: String, devCloudImage: DevCloudImage): String {
        val url = "$devCloudUrl/api/v2.1/images"
        val body = ObjectMapper().writeValueAsString(devCloudImage)
        logger.info("request body: $body")
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(addHeaders(staffName))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[$staffName] Create image failure with image ($devCloudImage) with response [${response.code()}|${response.message()}|$responseContent]")
                throw RuntimeException("Fail to createImage")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<*, *>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun getTasks(staffName: String, taskId: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/tasks/$taskId"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(addHeaders(staffName))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[$staffName|$taskId] Fail to get tasks with response [${response.code()}|${response.message()}|$responseContent]")
                throw OperationException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getContainerStatus(staffName: String, name: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$name/status"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(addHeaders(staffName))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("[$staffName|$name] Fail to get container status with response [${response.code()}|${response.message()}|$responseContent]")
                throw RuntimeException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }
}