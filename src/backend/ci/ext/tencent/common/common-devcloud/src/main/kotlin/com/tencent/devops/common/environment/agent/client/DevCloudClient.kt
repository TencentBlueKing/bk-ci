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

package com.tencent.devops.common.environment.agent.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.pojo.BuildFailureException
import com.tencent.devops.common.environment.agent.pojo.devcloud.Action
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainer
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainerStatus
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImage
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImageVersion
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudJobReq
import com.tencent.devops.common.environment.agent.pojo.devcloud.ErrorCodeEnum
import com.tencent.devops.common.environment.agent.pojo.devcloud.JobRequest
import com.tencent.devops.common.environment.agent.pojo.devcloud.JobResponse
import com.tencent.devops.common.environment.agent.pojo.devcloud.Params
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskStatus
import com.tencent.devops.common.environment.agent.pojo.devcloud.VolumeDetail
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.net.URLEncoder

@Suppress("ALL")
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

    @Value("\${devCloud.cpu:16}")
    var cpu: Int = 16

    @Value("\${devCloud.memory:32768M}")
    var memory: String = "32768M"

    fun createContainer(dispatchMessage: DispatchMessage, devCloudContainer: DevCloudContainer): Pair<String, String> {
        val url = "$devCloudUrl/api/v2.1/containers"
        val body = ObjectMapper().writeValueAsString(devCloudContainer)
        logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] request url: $url")
        logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(
                appId = devCloudAppId,
                token = devCloudToken,
                staffName = dispatchMessage.userId,
                proxyToken = smartProxyToken
            )))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info(
                    "[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}]code:${response.code()}, $responseContent"
                )
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                            "创建容器接口异常: Fail to createContainer, http response code: ${response.code()}"
                    )
                }

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
                val code = responseData["actionCode"] as Int
                if (200 == code) {
                    val dataMap = responseData["data"] as Map<String, Any>
                    return Pair((dataMap["taskId"] as Int).toString(), dataMap["name"] as String)
                } else {
                    val msg = responseData["actionMessage"] as String
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                        errorCode = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                        formatErrorMessage = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建容器接口返回失败: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] create container SocketTimeoutException", e
            )
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建容器接口超时, url: $url")
        }
    }

    fun operateContainer(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        action: Action,
        param: Params? = null
    ): String {
        val url = "$devCloudUrl/api/v2.1/containers/$name"
        val body = when (action) {
            Action.DELETE -> "{\"action\":\"delete\",\"params\":{}}"
            Action.STOP -> "{\"action\":\"stop\",\"params\":{}}"
            Action.START -> if (null != param) {
                "{\"action\":\"start\",\"params\": ${jacksonObjectMapper().writeValueAsString(param)}}"
            } else {
                "{\"action\":\"start\",\"params\":{}}"
            }
            else -> ""
        }
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        logger.info("[$buildId]|[$vmSeqId] request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                            "操作容器接口异常（Fail to $action docker, http response code: ${response.code()}"
                    )
                }
                logger.info("[$buildId]|[$vmSeqId] response: $responseContent")
                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
                val code = responseData["actionCode"] as Int
                if (200 == code) {
                    val dataMap = responseData["data"] as Map<String, Any>
                    return (dataMap["taskId"] as Int).toString()
                } else {
                    val msg = responseData["actionMessage"] as String
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                        errorCode = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                        formatErrorMessage = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 操作容器接口返回失败：$msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("[$buildId]|[$vmSeqId] operateContainer get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 操作容器接口超时, url: $url")
        }
    }

    fun getContainers(staffName: String, keyword: String?, page: Int, size: Int): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers?page=$page&size=$size" +
            if (null != keyword && keyword.isNotBlank()) {
                "&keyword=" + URLEncoder.encode(keyword, "UTF-8")
            } else {
                ""
            }
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get containers")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getContainerStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        retryTime: Int = 3
    ): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$name/status"
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("[$buildId]|[$vmSeqId] containerName: $name response: $responseContent")
                if (!response.isSuccessful) {
                    if (retryTime > 0) {
                        val retryTimeLocal = retryTime - 1
                        return getContainerStatus(buildId, vmSeqId, userId, name, retryTimeLocal)
                    }
                    // throw RuntimeException("Fail to get container status")
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                        errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 -" +
                            " 获取容器状态接口异常（Fail to get container status, http response code: ${response.code()}"
                    )
                }
                return JSONObject(responseContent)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info("[$buildId]|[$vmSeqId]$name getContainerStatus SocketTimeoutException. retry: $retryTime")
                return getContainerStatus(buildId, vmSeqId, userId, name, retryTime - 1)
            } else {
                logger.error("[$buildId]|[$vmSeqId] containerName: $name getContainerStatus failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取容器状态接口超时, url: $url")
            }
        }
    }

    fun getContainerInstance(staffName: String, id: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$id/instances"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get container status")
            }
            return JSONObject(responseContent)
        }
    }

    fun createImage(staffName: String, devCloudImage: DevCloudImage): String {
        val url = "$devCloudUrl/api/v2.1/images"
        val body = ObjectMapper().writeValueAsString(devCloudImage)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                // throw RuntimeException("Fail to createImage")
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                        "创建镜像接口异常（Fail to createImage, http response code: ${response.code()}"
                )
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                // throw OperationException(msg)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_FAIL.errorType,
                    errorCode = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_FAIL.errorCode,
                    formatErrorMessage = ErrorCodeEnum.CREATE_IMAGE_INTERFACE_FAIL.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建镜像接口返回失败：$msg"
                )
            }
        }
    }

    fun createImageVersions(staffName: String, id: String, devCloudImageVersion: DevCloudImageVersion): String {
        val url = devCloudUrl + "/api/v2.1/images/" + id + "/versions/" + devCloudImageVersion.version
        val body = ObjectMapper().writeValueAsString(devCloudImageVersion)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                // throw RuntimeException("Fail to createImageVersions")
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                        "创建镜像新版本接口异常（Fail to createImageVersions, http response code: ${response.code()}"
                )
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                // throw OperationException(msg)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_FAIL.errorType,
                    errorCode = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_FAIL.errorCode,
                    formatErrorMessage = ErrorCodeEnum.CREATE_IMAGE_VERSION_INTERFACE_FAIL.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建镜像新版本接口返回失败：$msg"
                )
            }
        }
    }

    fun getTasks(staffName: String, taskId: String, retryFlag: Int = 3): JSONObject {
        val url = "$devCloudUrl/api/v2.1/tasks/$taskId"
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Get task status failed, responseCode: ${response.code()}")

                    // 接口请求失败时，sleep 5s，再查一次
                    Thread.sleep(5 * 1000)
                    OkhttpUtils.doHttp(request).use {
                        val retryResponseContent = it.body()!!.string()
                        if (!it.isSuccessful) {
                            // 没机会了，只能失败
                            logger.error("$taskId retry get task status failed, retry responseCode: ${it.code()}")
                            throw BuildFailureException(
                                errorType = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                                errorCode = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                                formatErrorMessage = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                                errorMessage = "获取TASK状态接口异常：http response code: ${response.code()}"
                            )
                        }

                        logger.info("retry response: $retryResponseContent")
                        return JSONObject(retryResponseContent)
                    }
                }

                return JSONObject(responseContent)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryFlag > 0) {
                logger.info("$taskId get task SocketTimeoutException. retry: $retryFlag")
                return getTasks(staffName, taskId, retryFlag - 1)
            } else {
                logger.error("$taskId get task status failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取TASK状态接口超时, url: $url")
            }
        }
    }

    fun getWebsocket(staffName: String, containerName: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$containerName/terminal"
        logger.info("request url: $url, staffName: $staffName")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                // throw OperationException("Fail to get container websocket")
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 -" +
                        " 获取websocket接口异常（Fail to getWebsocket, http response code: ${response.code()}"
                )
            }
            return JSONObject(responseContent)
        }
    }

    fun createJob(
        userId: String,
        buildId: String,
        jobReq: DevCloudJobReq
    ): JobResponse {
        val jobRequestBody = JobRequest(
            alias = jobReq.alias,
            activeDeadlineSeconds = jobReq.activeDeadlineSeconds,
            image = jobReq.image,
            registry = jobReq.registry,
            params = jobReq.params,
            podNameSelector = jobReq.podNameSelector,
            mountPath = jobReq.mountPath,
            memory = memory,
            cpu = cpu
        )

        val url = "$devCloudUrl/api/v2.1/job"
        val body = JsonUtil.toJson(jobRequestBody)
        val request = Request.Builder().url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body)).build()
        val responseBody = OkhttpUtils.doHttp(request).body()!!.string()
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getJobStatus(userId: String, jobName: String): String {
        val url = "$devCloudUrl/api/v2.1/job/$jobName/status"
        logger.info("getJobStatus request url: $url, staffName: $userId")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                // throw OperationException("Fail to get container websocket")
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                        "获取websocket接口异常（Fail to getWebsocket, http response code: ${response.code()}"
                )
            }
            return responseContent
        }
    }

    fun getJobLogs(userId: String, jobName: String): String {
        val url = "$devCloudUrl/api/v2.1/job/$jobName/logs"
        logger.info("getJobStatus request url: $url, staffName: $userId")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                // throw OperationException("Fail to get container websocket")
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.WEBSOCKET_URL_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - " +
                        "获取websocket接口异常（Fail to getWebsocket, http response code: ${response.code()}"
                )
            }
            return responseContent
        }
    }

    fun executeContainerCommand(staffName: String, name: String, command: List<String>): Pair<Boolean, String> {
        val url = "$devCloudUrl/api/v2.1/containers/$name/exec"
        val body = mapOf("command" to command)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .post(RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                ObjectMapper().writeValueAsString(body))
            )
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to start docker")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as List<Map<String, Any>>
                val retCode = dataMap.last()["code"] as Int
                val retMsg = dataMap.last()["ret"] as String
                return if (0 == retCode) {
                    logger.info("execute command success")
                    Pair(true, retMsg)
                } else {
                    logger.info("execute command failed, retCode:$retCode, msg:$retMsg")
                    Pair(false, retMsg)
                }
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    /**
     * first： 成功or失败
     * second：成功时为containerName，失败时为错误信息
     */
    fun waitTaskFinish(userId: String, taskId: String): Triple<TaskStatus, String, ErrorCodeEnum> {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("$taskId dev cloud task timeout")
                return Triple(TaskStatus.TIMEOUT, "创建容器超时（10min）", ErrorCodeEnum.CREATE_VM_ERROR)
            }
            Thread.sleep(1 * 1000)
            val (isFinish, success, msg, errorCodeEnum) = getTaskResult(userId, taskId)
            return when {
                !isFinish -> continue@loop
                !success -> {
                    Triple(TaskStatus.FAILED, msg, errorCodeEnum)
                }
                else -> Triple(TaskStatus.SUCCEEDED, msg, errorCodeEnum)
            }
        }
    }

    private fun getTaskResult(userId: String, taskId: String): TaskResult {
        try {
            val taskResponse = getTasks(userId, taskId)
            val actionCode = taskResponse.optString("actionCode")
            return if ("200" != actionCode) {
                // 创建失败
                val msg = taskResponse.optString("actionMessage")
                logger.error("Execute task: $taskId failed, actionCode is $actionCode, msg: $msg")

                val errorInfo = taskResponse.optJSONObject("errorInfo")

                when {
                    // 5000200表示agent执行完关机导致的启动异常，这里忽略异常
                    errorInfo.optInt("code") == 5000200 -> {
                        TaskResult(isFinish = true, success = true, msg = msg)
                    }
                    errorInfo.optInt("type") == 0 -> {
                        TaskResult(
                            isFinish = true,
                            success = false,
                            msg = msg,
                            errorCodeEnum = ErrorCodeEnum.CREATE_VM_USER_ERROR
                        )
                    }
                    else -> {
                        TaskResult(isFinish = true, success = false, msg = msg)
                    }
                }
            } else {
                when (taskResponse.optJSONObject("data").optString("status")) {
                    "succeeded" -> {
                        val containerName = taskResponse.optJSONObject("data").optString("name")
                        logger.info("Task: $taskId success, containerName: $containerName, taskResponse: $taskResponse")
                        TaskResult(isFinish = true, success = true, msg = containerName)
                    }
                    "failed" -> {
                        val resultDisplay = taskResponse.optJSONObject("data")
                            .optJSONObject("result")
                            .optJSONArray("logs")
                        logger.error("Task: $taskId failed, taskResponse: $taskResponse")
                        TaskResult(isFinish = true, success = false, msg = formatDevcloudLogList(resultDisplay))
                    }
                    else -> TaskResult(isFinish = false, success = false, msg = "")
                }
            }
        } catch (e: Exception) {
            logger.error("Get dev cloud task error, taskId: $taskId", e)
            return TaskResult(isFinish = true, success = false, msg = "创建失败，异常信息:${e.message}")
        }
    }

    private fun formatDevcloudLogList(jsonArray: JSONArray): String {
        return try {
            val logFormat = StringBuilder("\n")
            for (i in 0 until jsonArray.length()) {
                val log = jsonArray.get(i) as String
                logFormat.append(log + "\n")
            }

            logFormat.toString()
        } catch (e: Exception) {
            logger.error("formatDevcloudLogList error.", e)
            jsonArray.toString()
        }
    }

    /**
     * first： 成功or失败
     */
    fun waitContainerRunning(
        buildId: String,
        vmSeqId: String,
        userId: String,
        containerName: String
    ): DevCloudContainerStatus {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("dev cloud container start timeout")
                return DevCloudContainerStatus.EXCEPTION
            }
            Thread.sleep(1 * 1000)

            // 轮询容器状态
            var success = true
            var isFinish = false
            val statusResponse = getContainerStatus(buildId, vmSeqId, userId, containerName)
            val actionCode = statusResponse.optInt("actionCode")
            if (actionCode == 200) {
                val status = statusResponse.optString("data")
                if (status == "running") {
                    isFinish = true
                }
            } else {
                success = false
            }

            return when {
                !isFinish -> continue@loop
                !success -> {
                    logger.error("execute job failed, msg: $statusResponse")
                    DevCloudContainerStatus.EXCEPTION
                }
                else -> DevCloudContainerStatus.RUNNING
            }
        }
    }

    fun createCfs(staffName: String, name: String, volume: Int, description: String): Pair<String, Int> {
        val url = "$devCloudUrl/api/v2.1/cfs"

        val requestData = mapOf(
            "name" to name,
            "volume" to volume,
            "description" to description
        )
        val body = ObjectMapper().writeValueAsString(requestData)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
            .url(url)
            // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to createImageVersions")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return Pair((dataMap["taskId"] as Int).toString(), dataMap["cfsId"] as Int)
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun getCfsDetail(staffName: String, volumeId: Int): VolumeDetail {
        val url = "$devCloudUrl/api/v2.1/cfs/$volumeId"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get cfs detail, error code: ${response.code()}")
                throw RuntimeException("Fail to get cfs detail")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val volumeDetail: VolumeDetail = JsonUtil.to(responseData["data"].toString(), VolumeDetail::class.java)
            return if (volumeDetail.defaultMountIp.isNullOrBlank()) {
                VolumeDetail(
                    volumeDetail.id,
                    volumeDetail.name,
                    volumeDetail.volume,
                    volumeDetail.description,
                    volumeDetail.mountTargets[0].ip,
                    volumeDetail.mountTargets
                )
            } else {
                volumeDetail
            }
        }
    }

    fun deleteCfs(userId: String, volumeId: Int?): String {
        val url = "$devCloudUrl/api/v2.1/cfs/$volumeId"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, userId)))
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)))
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to delete cfs")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }
}

data class TaskResult(
    val isFinish: Boolean,
    val success: Boolean,
    val msg: String,
    val errorCodeEnum: ErrorCodeEnum = ErrorCodeEnum.CREATE_VM_ERROR
)
