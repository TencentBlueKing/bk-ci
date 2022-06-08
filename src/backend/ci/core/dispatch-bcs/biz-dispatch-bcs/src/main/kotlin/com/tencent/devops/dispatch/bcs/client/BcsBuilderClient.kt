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
import com.tencent.devops.dispatch.bcs.common.ConstantsMessage
import com.tencent.devops.dispatch.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuildImageReq
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuilder
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuilderStatus
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuilderStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsOperateBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsResult
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.bcs.isRunning
import com.tencent.devops.dispatch.bcs.pojo.bcs.resp.BcsTaskResp
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

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
                val responseContent = response.body()!!.string()
                logger.info("[$buildId]|[$vmSeqId] builderName: $name response: $responseContent")
                if (response.isSuccessful) {
                    return objectMapper.readValue(responseContent)
                }

                if (retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getBuilderDetail(buildId, vmSeqId, userId, name, retryTimeLocal)
                }

                throw BuildFailureException(
                    ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                    ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                    ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}获取构建机详情接口异常" +
                            "（Fail to get builder detail, http response code: ${response.code()}"
                )
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info("[$buildId]|[$vmSeqId] builderName: $name getBuilderDetail SocketTimeoutException." +
                        " retry:$retryTime")
                return getBuilderDetail(buildId, vmSeqId, userId, name, retryTime - 1)
            } else {
                logger.error("[$buildId]|[$vmSeqId] builderName: $name getBuilderDetail failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取构建机详情接口超时, url: $url"
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
                    .delete(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
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
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                    .build(),
                "start"
            )
            else -> return ""
        }
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        logger.info("[$buildId]|[$vmSeqId] request body: $body")
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}操作构建机接口异常" +
                            "（Fail to $action docker, http response code: ${response.code()}"
                    )
                }
                logger.info("[$buildId]|[$vmSeqId] response: $responseContent")
                val responseData: BcsResult<BcsTaskResp> = objectMapper.readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}操作构建机接口返回失败：$msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("[$buildId]|[$vmSeqId] operateBuilder get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "${ConstantsMessage.TROUBLE_SHOOTING}操作构建机接口超时, url: $url"
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
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("[$buildId]|[$vmSeqId] http code is ${response.code()}, $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}创建构建机接口异常: Fail to createBuilder, http response code: " +
                            "${response.code()}"
                    )
                }

                val responseData: BcsResult<BcsTaskResp> = jacksonObjectMapper().readValue(responseContent)
                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING}创建构建机接口返回失败: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "[$buildId]|[$vmSeqId] create builder get SocketTimeoutException",
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
                val responseContent = response.body()!!.string()
                logger.info("response: $responseContent")
                if (!response.isSuccessful) {
                    // throw OperationException("Fail to get container websocket")
                    throw BuildFailureException(
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                        "获取websocket接口异常（Fail to getWebsocket, http response code: ${response.code()}"
                    )
                }

                val bcsResult: BcsResult<String> = objectMapper.readValue(responseContent)
                if (bcsResult.result == null || !bcsResult.result!!) {
                    throw RuntimeException(bcsResult.message)
                }

                return bcsResult
            }
        } catch (e: Exception) {
            logger.error("[$projectId]|[$pipelineId] builderName: $builderName getWebsocketUrl failed.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                errorMessage = "获取登录调试链接接口超时, url: $url, ${e.message}"
            )
        }
    }

    fun buildAndPushImage(
        userId: String,
        builderName: String,
        bcsBuildImageReq: BcsBuildImageReq
    ): String {
        val url = "/api/v1/devops/builder/$builderName/images"
        logger.info("Build and push image, request url: $url, staffName: $userId")

        val request = clientCommon.baseRequest(userId, url)
            .post(RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(bcsBuildImageReq)))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("$userId build and push image response: $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.formatErrorMessage,
                        "构建并推送接口异常（Fail to build image, http response code: ${response.code()}"
                    )
                }
                val responseData: BcsResult<BcsTaskResp> = objectMapper.readValue(responseContent)

                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "${ConstantsMessage.TROUBLE_SHOOTING} 构建并镜像接口返回失败: $msg"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("$userId builderName: $builderName build and push image failed.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                errorMessage = "构建并推送接口超时, url: $url"
            )
        }
    }
}
