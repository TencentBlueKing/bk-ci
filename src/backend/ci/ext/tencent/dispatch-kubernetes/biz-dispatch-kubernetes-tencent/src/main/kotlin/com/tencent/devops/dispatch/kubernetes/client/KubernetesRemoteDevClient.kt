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

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.common.ConstantsMessage
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.JobStatus
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesWorkspace
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesWorkspaceUrlRsp
import com.tencent.devops.dispatch.kubernetes.pojo.TaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.getCodeMessage
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class KubernetesRemoteDevClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesRemoteDevClient::class.java)
    }

    fun createWorkspace(
        userId: String,
        kubernetesWorkspace: KubernetesWorkspace
    ): String {
        val url = "/api/remoting/workspaces"
        val body = JsonUtil.toJson(kubernetesWorkspace)
        logger.info("Create workspace request url: $url, body: $body")
        val request = clientCommon.baseRequest(userId, url).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body
            )
        ).build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId create workspace response: ${response.code}, $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}创建工作空间异常: Fail to create workspace, http response code: " +
                            "${response.code}"
                    )
                }

                val responseData: KubernetesResult<TaskResp> = jacksonObjectMapper().readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}创建工作空间接口返回失败: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "$userId create workspace get SocketTimeoutException",
                e
            )
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "${ConstantsMessage.TROUBLE_SHOOTING}创建构建机接口超时, url: $url"
            )
        }
    }

    fun getJobStatus(userId: String, jobName: String): KubernetesResult<JobStatus> {
        val url = "/api/jobs/$jobName/status"
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("Get job: $jobName status request url: $url, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get job: $jobName status response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}查询Job status接口异常（Fail to getJobStatus, " +
                        "http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): KubernetesResult<String> {
        val url = "/api/jobs/$jobName/log".also {
            if (sinceTime != null) {
                it.plus("?sinceTime=$sinceTime")
            }
        }
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info(
            "Get job: $jobName logs request url: $url, jobName: $jobName, " +
                "sinceTime: $sinceTime, staffName: $userId"
        )
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get job: $jobName logs response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}获取Job logs接口异常" +
                        "（Fail to getJobLogs, http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getWorkspaceUrl(
        userId: String,
        workspaceName: String
    ): String? {
        val url = "/api/remoting/workspaces/$workspaceName/urls"
        logger.info("$userId|$workspaceName Get workspaceUrl request url: $url")

        val request = clientCommon.baseRequest(userId, url).get().build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId|$workspaceName get workspaceUrl response: $responseContent")
                if (!response.isSuccessful) {
                    // throw OperationException("Fail to get container websocket")
                    throw BuildFailureException(
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                        "获取工作空间url接口异常（Fail to get workspaceUrl, http response code: ${response.code}"
                    )
                }
                val result: KubernetesResult<KubernetesWorkspaceUrlRsp> = objectMapper.readValue(responseContent)
                if (result.isNotOk()) {
                    throw RuntimeException(result.message)
                }

                return result.data!!.webVscodeUrl
            }
        } catch (e: Exception) {
            logger.error("[$userId $workspaceName get workspaceUrl failed.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                errorMessage = "获取工作空间链接接口超时, url: $url, ${e.message}"
            )
        }
    }
}
