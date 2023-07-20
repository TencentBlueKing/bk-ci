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

package com.tencent.devops.dispatch.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilder
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilderStatus
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilderStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsOperateBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsResult
import com.tencent.devops.dispatch.bcs.pojo.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.isRunning
import com.tencent.devops.dispatch.bcs.pojo.resp.BcsTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.BK_MACHINE_INTERFACE_TIMEOUT
import com.tencent.devops.dispatch.kubernetes.pojo.BK_TROUBLE_SHOOTING
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BcsBuilderClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: BcsClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsBuilderClient::class.java)
    }

    fun getBuilderDetail(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        retryTime: Int = 3
    ): BcsResult<BcsBuilderStatus> {
        val url = "/api/v1/devops/builder/$name"
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        val request = clientCommon.baseRequest(userId, url).get().build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("[$buildId]|[$vmSeqId] builderName: $name response: $responseContent")
                if (response.isSuccessful) {
                    return objectMapper.readValue(responseContent)
                }

                if (retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getBuilderDetail(buildId, vmSeqId, userId, name, retryTimeLocal)
                }
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                        "Fail to get builder detail, http response code: ${response.code}"
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
                val errorMsg = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage()
                    logger.error("[$buildId]|[$vmSeqId] builderName: $name getBuilderDetail failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = errorMsg,
                    errorMessage = "$errorMsg, url: $url"
                )
            }
        }
    }

    fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        param: BcsOperateBuilderParams
    ): String {
        val url = "/api/v1/devops/builder/$name"
        val body = ObjectMapper().writeValueAsString(param)
        val (request, action) = when (param) {
            is BcsDeleteBuilderParams -> Pair(
                clientCommon.baseRequest(userId, url)
                    .delete(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
                    .build(),
                "delete"
            )
            is BcsStopBuilderParams -> Pair(
                clientCommon.baseRequest(userId, "$url/stop")
                    .post(RequestBody.create(null, ""))
                    .build(),
                "stop"
            )
            is BcsStartBuilderParams -> Pair(
                clientCommon.baseRequest(userId, "$url/start")
                    .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
                    .build(),
                "start"
            )
            else -> return ""
        }
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        logger.info("[$buildId]|[$vmSeqId] request body: $body")
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    val errorMsg = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.getErrorMessage()
                        throw BuildFailureException(
                            ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorType,
                            ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorCode,
                            errorMsg,
                            "（Fail to $action docker, http response code: ${response.code}"
                    )
                }
                logger.info("[$buildId]|[$vmSeqId] response: $responseContent")
                val responseData: BcsResult<BcsTaskResp> = objectMapper.readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    val errorMsg = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                        errorMsg,
                        "$errorMsg：$msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("[$buildId]|[$vmSeqId] operateBuilder get SocketTimeoutException.", e)
            val errorMsg = I18nUtil.getCodeLanMessage(BK_TROUBLE_SHOOTING) +
                    I18nUtil.getCodeLanMessage(BK_MACHINE_INTERFACE_TIMEOUT)
                throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = errorMsg,
                errorMessage = "$errorMsg, url: $url"
            )
        }
    }

    fun createBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        bcsBuilder: BcsBuilder
    ): String {
        val url = "/api/v1/devops/builder/${bcsBuilder.name}"
        val body = ObjectMapper().writeValueAsString(bcsBuilder)
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        logger.info("[$buildId]|[$vmSeqId] request body: $body")
        val request = clientCommon.baseRequest(userId, url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("[$buildId]|[$vmSeqId] http code is ${response.code}, $responseContent")
                if (!response.isSuccessful) {
                    val errorMsg = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_ERROR.errorCode,
                        errorMsg,
                        errorMsg + ": Fail to createBuilder, http response code: ${response.code}"
                    )
                }

                val responseData: BcsResult<BcsTaskResp> = jacksonObjectMapper().readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    val errorMsg = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                        errorMsg,
                        "$errorMsg: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "[$buildId]|[$vmSeqId] create builder get SocketTimeoutException",
                e
            )
            val errorMsg = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage()
                throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = errorMsg,
                errorMessage = "$errorMsg, url: $url"
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
    ): BcsBuilderStatusEnum {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("dev cloud container start timeout")
                return BcsBuilderStatusEnum.ERROR
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
                    BcsBuilderStatusEnum.ERROR
                }
                else -> BcsBuilderStatusEnum.RUNNING
            }
        }
    }

    fun getWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): BcsResult<String> {
        val url = "/api/v1/devops/builder/$builderName/terminal"
        logger.info("request url: $url, staffName: $staffName")

        val request = clientCommon.baseRequest(staffName, url)
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("response: $responseContent")
                if (!response.isSuccessful) {
                    // throw OperationException("Fail to get container websocket")
                    val errorMsg = "${ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode}"
                        throw BuildFailureException(
                        ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                            errorMsg,
                        "$errorMsg（Fail to getWebsocket, http response code: ${response.code}"
                    )
                }
                val bcsResult: BcsResult<String> = objectMapper.readValue(responseContent)
                if (bcsResult.result == null || !bcsResult.result) {
                    throw RuntimeException(bcsResult.message)
                }

                return bcsResult
            }
        } catch (e: Exception) {
            logger.error("[$projectId]|[$pipelineId] builderName: $builderName getWebsocketUrl failed.", e)
            val errorMsg = "${ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode}"
                throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.BCS_WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                formatErrorMessage = errorMsg,
                errorMessage = "$errorMsg, url: $url, ${e.message}"
            )
        }
    }

    fun buildAndPushImage(
        userId: String,
        buildImageReq: DispatchBuildImageReq
    ): String {
        val builderName = "" // TODO
        val url = "/api/v1/devops/builder/$builderName/images"
        logger.info("Build and push image, request url: $url, staffName: $userId")

        val request = clientCommon.baseRequest(userId, url)
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(buildImageReq)
                )
            )
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId build and push image response: $responseContent")
                val errorMsg = "${ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.errorCode}"
                    if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.errorCode,
                        errorMsg,
                        "$errorMsg（Fail to build image, http response code: ${response.code}"
                    )
                }
                val responseData: BcsResult<BcsTaskResp> = objectMapper.readValue(responseContent)

                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage(),
                        "$errorMsg: $msg"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("$userId builderName: $builderName build and push image failed.", e)
            val errorMsg = "${ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode}"
                throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                formatErrorMessage = errorMsg,
                errorMessage = "errorMsg, url: $url"
            )
        }
    }
}
