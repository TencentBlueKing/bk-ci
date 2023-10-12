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
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.Builder
import com.tencent.devops.dispatch.kubernetes.pojo.DeleteBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesBuilderStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.OperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.StartBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.StopBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.TaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.getCodeMessage
import com.tencent.devops.dispatch.kubernetes.pojo.isRunning
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KubernetesBuilderClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesBuilderClient::class.java)
    }

    fun getBuilderDetail(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        retryTime: Int = 3
    ): KubernetesResult<KubernetesBuilderStatus> {
        val url = "/api/builders/$name/status"
        logger.info("[$buildId]|[$vmSeqId] Get detail builderName: $name request url: $url")
        val request = clientCommon.baseRequest(userId, url).get().build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("[$buildId]|[$vmSeqId] Get detail builderName: $name response: $responseContent")
                if (response.isSuccessful) {
                    return objectMapper.readValue(responseContent)
                }

                if (retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getBuilderDetail(buildId, vmSeqId, userId, name, retryTimeLocal)
                }

                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "Fail to get builder detail, http response code: ${response.code}"
                )
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info(
                    "[$buildId]|[$vmSeqId] builderName: $name getBuilderDetail SocketTimeoutException." +
                        " retry:$retryTime"
                )
                return getBuilderDetail(buildId, vmSeqId, userId, name, retryTime - 1)
            } else {
                logger.error("[$buildId]|[$vmSeqId] builderName: $name getBuilderDetail failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "The interface timed out to obtain the builder details, url: $url"
                )
            }
        }
    }

    fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        param: OperateBuilderParams
    ): String {
        val url = "/api/builders/$name"
        val body = ObjectMapper().writeValueAsString(param)
        val (request, action) = when (param) {
            is DeleteBuilderParams -> Pair(
                clientCommon.baseRequest(userId, url)
                    .delete(RequestBody.create(null, ""))
                    .build(),
                ""
            )

            is StopBuilderParams -> Pair(
                clientCommon.baseRequest(userId, "$url/stop")
                    .put(RequestBody.create(null, ""))
                    .build(),
                "stop"
            )

            is StartBuilderParams -> Pair(
                clientCommon.baseRequest(userId, "$url/start")
                    .put(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
                    .build(),
                "start"
            )

            else -> return ""
        }
        logger.info("[$buildId]|[$vmSeqId] operator builder: $name request url: $url")
        logger.info("[$buildId]|[$vmSeqId] operator builder: $name request body: $body")
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.getErrorMessage(),
                        errorMessage = "Fail to $action docker, http response code: ${response.code}"
                    )
                }
                logger.info("[$buildId]|[$vmSeqId] operator builder: $name response: $responseContent")
                val responseData: KubernetesResult<TaskResp> = objectMapper.readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                        errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.getErrorMessage(),
                        errorMessage = "The operation builder interface returns a failure：$msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("[$buildId]|[$vmSeqId] operateBuilder get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.getErrorMessage(),
                errorMessage = "The operation builder interface timed out, url: $url"
            )
        }
    }

    fun createBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builder: Builder
    ): String {
        val url = "/api/builders"
        val body = ObjectMapper().writeValueAsString(builder)
        logger.info("[$buildId]|[$vmSeqId] create builder request url: $url")
        logger.info("[$buildId]|[$vmSeqId] create builder request body: $body")
        val request = clientCommon.baseRequest(userId, url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("[$buildId]|[$vmSeqId] create builder response: ${response.code}, $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.getErrorMessage(),
                        errorMessage = "Fail to createBuilder, http response code: ${response.code}"
                    )
                }

                val responseData: KubernetesResult<TaskResp> = jacksonObjectMapper().readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                        errorCode = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage(),
                        errorMessage = "Failed to create builder interface: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "[$buildId]|[$vmSeqId] create builder get SocketTimeoutException",
                e
            )
            throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage(),
                errorMessage = "Creating a builder interface times out, url: $url"
            )
        }
    }

    fun waitContainerRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        containerName: String
    ): KubernetesBuilderStatusEnum {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("$buildId|$vmSeqId|$containerName start builder running timeout")
                return KubernetesBuilderStatusEnum.FAILED
            }
            Thread.sleep(1 * 1000)

            // 轮询容器状态
            var success = true
            var isFinish = false
            val statusResponse = getBuilderDetail(buildId, vmSeqId, userId, containerName)
            if (statusResponse.isOk()) {
                val status = statusResponse.data!!
                if (status.isRunning()) {
                    isFinish = true
                }
            } else {
                success = false
            }

            return when {
                !isFinish -> continue@loop
                !success -> {
                    logger.error("execute job failed, msg: $statusResponse")
                    KubernetesBuilderStatusEnum.FAILED
                }

                else -> KubernetesBuilderStatusEnum.RUNNING
            }
        }
    }

    fun getWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): KubernetesResult<String> {
        val url = "/api/builders/$builderName/terminal"
        logger.info("$projectId|$staffName|$builderName Get websocketUrl request url: $url, staffName: $staffName")

        val request = clientCommon.baseRequest(staffName, url)
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$projectId|$staffName|$builderName Get websocketUrl response: $responseContent")
                if (!response.isSuccessful) {
                    // throw OperationException("Fail to get container websocket")
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.getErrorMessage(),
                        errorMessage = "Fail to getWebsocket, http response code: ${response.code}"
                    )
                }
                val result: KubernetesResult<String> = objectMapper.readValue(responseContent)
                if (result.isNotOk()) {
                    throw RuntimeException(result.message)
                }

                return result
            }
        } catch (e: Exception) {
            logger.error("[$projectId]|[$pipelineId] builderName: $builderName getWebsocketUrl failed.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.getErrorMessage(),
                errorMessage = "Getting the login debug link interface timed out, url: $url, ${e.message}"
            )
        }
    }
}
