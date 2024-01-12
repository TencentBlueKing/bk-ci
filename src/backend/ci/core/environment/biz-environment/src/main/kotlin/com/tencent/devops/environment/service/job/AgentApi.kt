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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.agentres.AgentAgentResult
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component("AgentApi")
class AgentApi {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String = ""

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String = ""

    @Value("\${job.nodemanApiBaseUrl:}")
    private val nodemanApiBaseUrl = ""

    @Value("\${job.installAgentPath:#{\"/job/install\"}}")
    private val installAgentPath = ""

    @Value("\${job.queryAgentTaskStatusPath:#{\"/details\"}}") // 前面要拼 /job/{jobId}
    private val queryAgentTaskStatusPath = ""

    @Value("\${job.queryAgentStatusFromNodemanPath:#{\"/search\"}}")
    private val queryAgentStatusFromNodemanPath = ""

    @Value("\${job.queryAgentTaskLogPath:#{\"/log\"}}") // 前面要拼 /job/{jobId}
    private val queryAgentTaskLogPath = ""

    @Value("\${job.terminalAgentInstallTaskPath:#{\"/revoke\"}}") // 前面要拼 /job/{jobId}
    private val terminalAgentInstallTaskPath = ""

    @Value("\${job.retryAgentInstallTaskPath:#{\"/retry\"}}") // 前面要拼 /job/{jobId}
    private val retryAgentInstallTaskPath = ""

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000
        private const val JOB_PERFIX = "/job"
        private const val HOST_PERFIX = "/host"
        private const val QUERY_AGENT_LOG = "/?instance_id=%s"

        private val logger = LoggerFactory.getLogger(AgentApi::class.java)

        private val threadLocal = ThreadLocal<String>()
        fun setThreadLocal(value: String) {
            threadLocal.set(value)
        }

        fun getThreadLocal(): String? {
            return threadLocal.get()
        }

        fun removeThreadLocal() {
            threadLocal.remove()
        }
    }

    fun <T, U : Any> executePostRequest(req: T, classOfU: Class<U>, jobId: Int? = null): AgentAgentResult<U> {
        val (bkAuthorization, url) = getAgentAuthReq(jobId)
        val headers = getAuthHeaderMap(bkAuthorization)
        val requestContent = jacksonObjectMapper().writeValueAsString(req)
        if (logger.isDebugEnabled)
            logger.debug("[${getThreadLocal()}] headers: $headers, url: $url, body: $requestContent")
        val resultFromRes = getResultFromRes(OkhttpUtils.doPost(url, requestContent, headers), classOfU)
        if (logger.isDebugEnabled) logger.debug("[executePostRequest] resultFromRes: $resultFromRes")
        return resultFromRes
    }

    fun <T, U> executeGetRequest(classOfT: Class<T>, jobId: Int? = null, vararg args: U): AgentAgentResult<T> {
        val operationName = getThreadLocal()
        val (bkAuthorization, url) = getAgentAuthReq(jobId)
        val headers = getAuthHeaderMap(bkAuthorization)
        val suffix = when (operationName) {
            "queryAgentTaskLog" -> QUERY_AGENT_LOG
            else -> ""
        }
        val urlWithSuffix = url + String.format(suffix, *args)
        if (logger.isDebugEnabled)
            logger.debug("[$operationName] headers: ${logWithLengthLimit(headers.toString())}, url: $urlWithSuffix")
        return getResultFromRes(OkhttpUtils.doGet(urlWithSuffix, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): AgentAgentResult<T> {
        val operationName = getThreadLocal()
        removeThreadLocal()
        try {
            val responseBody = response.body?.string()
            if (logger.isDebugEnabled) {
                val responseLog = logWithLengthLimit(responseBody.toString())
                logger.debug("[$operationName] response: $response")
                logger.debug("[$operationName] responseBody: $responseBody")
                logger.debug("[$operationName] response body(origin): $responseLog")
            }

            val agentResp = jacksonObjectMapper().readValue<AgentAgentResult<T>>(responseBody!!)
            if (logger.isDebugEnabled)
                logger.debug(
                    "[$operationName] response body(deserialized AgentResult<T>): " +
                        logWithLengthLimit(agentResp.toString())
                )
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
                        jsonData = jacksonObjectMapper().writeValueAsString(agentResp.data)
                        jacksonObjectMapper().readValue(jsonData, classOfT)
                    } else {
                        null
                    }
                if (logger.isDebugEnabled) {
                    logger.debug("[$operationName] operationResult type: " + operationResult!!::class)
                    logger.debug("[$operationName] serialized jsonData: ${logWithLengthLimit(jsonData)}")
                    logger.debug(
                        "[$operationName] ${operationName}Result: " +
                            logWithLengthLimit(operationResult.toString())
                    )
                }
                val agentAgentResult = AgentAgentResult(
                    code = agentResp.code,
                    result = agentResp.result,
                    message = agentResp.message,
                    errors = agentResp.errors,
                    data = operationResult
                )
                if (logger.isDebugEnabled)
                    logger.debug("[$operationName]agentResult: " + logWithLengthLimit(agentAgentResult.toString()))
                return agentAgentResult
            }
        } catch (exception: Exception) {
            logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
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
        val operationName = getThreadLocal()
        if (logger.isDebugEnabled) logger.debug("[getAgentAuthReq]operationName: $operationName")
        val url = when (operationName) {
            "installAgent" -> nodemanApiBaseUrl + installAgentPath
            "queryAgentTaskStatus" -> nodemanApiBaseUrl + JOB_PERFIX + jobId + queryAgentTaskStatusPath
            "queryAgentTaskLog" -> nodemanApiBaseUrl + JOB_PERFIX + jobId + queryAgentTaskLogPath
            "terminalAgentInstallTask" -> nodemanApiBaseUrl + JOB_PERFIX + jobId + terminalAgentInstallTaskPath
            "retryAgentInstallTask" -> nodemanApiBaseUrl + JOB_PERFIX + jobId + retryAgentInstallTaskPath
            "queryAgentStatusFromNodeman" -> nodemanApiBaseUrl + HOST_PERFIX + queryAgentStatusFromNodemanPath

            else -> ""
        }
        if (logger.isDebugEnabled) logger.debug("[getAgentAuthReq]url: $url")
        return Pair(bkAuthorization, url)
    }
}