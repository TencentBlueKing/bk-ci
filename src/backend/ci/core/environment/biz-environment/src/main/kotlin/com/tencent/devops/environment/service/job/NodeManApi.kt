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

package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component("NodeManApi")
class NodeManApi {
    @Value("\${environment.apigw.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${environment.apigw.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${environment.cc.bkScopeType:#{null}}")
    val bkScopeType: String = ""

    @Value("\${environment.cc.bkScopeId:#{null}}")
    val bkScopeId: String = ""

    @Value("\${environment.nodeman.nodemanApiBaseUrl:}")
    private val nodemanApiBaseUrl = ""

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        private val url = mapOf(
            "installAgent" to "/job/install",
            "queryAgentTaskStatus" to "/job/%s/details",
            "queryAgentTaskLog" to "/job/%s/log",
            "terminalAgentInstallTask" to "/job/%s/revoke",
            "retryAgentInstallTask" to "/job/%s/retry",
            "queryAgentStatusFromNodeman" to "/host/search",
            "queryAgentInstallChannel" to "/install_channel"
        )
        private val suffix = mapOf(
            "queryAgentTaskLog" to "/?instance_id=%s",
            "queryAgentInstallChannel" to "/?with_hidden=%s"
        )

        private val logger = LoggerFactory.getLogger(NodeManApi::class.java)

        private val nodemanOperationName = ThreadLocal<String>()
        fun setNodemanOperationName(value: String) {
            nodemanOperationName.set(value)
        }

        fun getNodemanOperationName(): String? {
            return nodemanOperationName.get()
        }

        fun removeNodemanOperationName() {
            nodemanOperationName.remove()
        }

        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    fun <T, U : Any> executePostRequest(req: T, classOfU: Class<U>, jobId: Int? = null): AgentOriginalResult<U> {
        val (bkAuthorization, url) = getAgentAuthReq(jobId)
        val headers = getAuthHeaderMap(bkAuthorization)
        val requestContent = mapper.writeValueAsString(req)
        logger.info("[${getNodemanOperationName()}]POST url: $url, body: ${logWithLengthLimit(requestContent)}")
        return getResultFromRes(OkhttpUtils.doPost(url, requestContent, headers), classOfU)
    }

    fun <T, U> executeGetRequest(classOfT: Class<T>, jobId: Int? = null, vararg args: U): AgentOriginalResult<T> {
        val operationName = getNodemanOperationName()
        val (bkAuthorization, url) = getAgentAuthReq(jobId)
        val headers = getAuthHeaderMap(bkAuthorization)
        val urlWithSuffix = url + String.format(suffix[operationName] ?: "", *args)
        logger.info("[$operationName]GET url: $urlWithSuffix")
        return getResultFromRes(OkhttpUtils.doGet(urlWithSuffix, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): AgentOriginalResult<T> {
        val operationName = getNodemanOperationName()
        removeNodemanOperationName()
        try {
            val responseBody = response.body?.string()
            logger.info("[$operationName] response body(origin): ${logWithLengthLimit(responseBody.toString())}")
            val agentResp = mapper.readValue<AgentOriginalResult<T>>(responseBody!!)
            if (!agentResp.result!!) {
                logger.error(
                    "[$operationName] Execute failed! Error code: ${agentResp.code}, " +
                        "Error msg: ${agentResp.message}"
                )
                throw RemoteServiceException(
                    "Execute failed! Error code: ${agentResp.code}, " +
                        "Error msg: ${agentResp.message}"
                )
            } else {
                var jsonData = ""
                val operationResult: T? =
                    if (null != agentResp.data) {
                        jsonData = mapper.writeValueAsString(agentResp.data)
                        mapper.readValue(jsonData, classOfT)
                    } else {
                        null
                    }
                if (logger.isDebugEnabled)
                    logger.debug("[$operationName] serialized jsonData: ${logWithLengthLimit(jsonData)}")
                return AgentOriginalResult(
                    code = agentResp.code,
                    result = agentResp.result,
                    message = agentResp.message,
                    errors = agentResp.errors,
                    data = operationResult
                )
            }
        } catch (exception: Exception) {
            logger.warn("Failed to execute the HTTP request. Exception:", exception)
            throw exception
        }
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }

    private fun getAuthHeaderMap(bkAuthorization: String): MutableMap<String, String> {
        return mutableMapOf(
            "accept" to "*/*",
            "Content-Type" to "application/json",
            "X-Bkapi-Authorization" to bkAuthorization
        )
    }

    private fun getAgentAuthReq(jobId: Int? = null): Pair<String, String> {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"$AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE\"}"
        val operationName = getNodemanOperationName()
        val reqUrl = nodemanApiBaseUrl + String.format(
            url[operationName] ?: "",
            jobId?.toString() ?: ""
        )
        return Pair(bkAuthorization, reqUrl)
    }
}