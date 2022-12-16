package com.tencent.devops.dispatch.devcloud.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
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

@Component
class DispatchDevCloudClient {
    private val logger = LoggerFactory.getLogger(DispatchDevCloudClient::class.java)

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""

    @Value("\${devCloud.smartProxyToken}")
    val smartProxyToken: String = ""

    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    fun createWorkspace(dispatchMessage: DispatchMessage, devCloudContainer: DevCloudContainer): Pair<String, String> {
        val url = devCloudUrl + "/api/v2.1/containers"
        val body = ObjectMapper().writeValueAsString(devCloudContainer)
        logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] request url: $url")
        logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] request body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                Headers.of(
                    SmartProxyUtil.makeHeaders(
                        devCloudAppId,
                        devCloudToken,
                        dispatchMessage.userId,
                        smartProxyToken,
                        dispatchMessage.projectId,
                        dispatchMessage.pipelineId
                    )
                )
            )
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] http code is ${response.code()}, $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建容器接口异常: Fail to createContainer, http response code: ${response.code()}"
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
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建容器接口返回失败: $msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error(
                "[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] create container get SocketTimeoutException",
                e
            )
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 创建容器接口超时, url: $url"
            )
        }
    }

    fun stopWorkspace(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        action: Action,
        param: Params? = null
    ): String {
        val url = devCloudUrl + "/api/v2.1/containers/" + name
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
            .headers(
                Headers.of(
                    SmartProxyUtil.makeHeaders(
                        devCloudAppId,
                        devCloudToken,
                        userId,
                        smartProxyToken,
                        projectId,
                        pipelineId
                    )
                )
            )
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 操作容器接口异常（Fail to $action docker, http response code: ${response.code()}"
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
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 操作容器接口返回失败：$msg"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("[$buildId]|[$vmSeqId] operateContainer get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.OPERATE_VM_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 操作容器接口超时, url: $url"
            )
        }
    }

    fun deleteWorkspace() {

    }

    fun getWorkspaceStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        retryTime: Int = 3
    ): JSONObject {
        val url = devCloudUrl + "/api/v2.1/containers/" + name + "/status"
        logger.info("[$buildId]|[$vmSeqId] request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(
                Headers.of(
                    SmartProxyUtil.makeHeaders(
                        devCloudAppId,
                        devCloudToken,
                        userId,
                        smartProxyToken,
                        projectId,
                        pipelineId
                    )
                )
            )
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("[$buildId]|[$vmSeqId] containerName: $name response: $responseContent")
                if (!response.isSuccessful) {
                    if (retryTime > 0) {
                        val retryTimeLocal = retryTime - 1
                        return getContainerStatus(projectId, pipelineId, buildId, vmSeqId, userId, name, retryTimeLocal)
                    }
                    // throw RuntimeException("Fail to get container status")
                    throw BuildFailureException(
                        ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 获取容器状态接口异常（Fail to get container" +
                            " status, http response code: ${response.code()}"
                    )
                }
                return JSONObject(responseContent)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info(
                    "[$buildId]|[$vmSeqId] containerName: $name getContainerStatus SocketTimeoutException. " +
                        "retry: $retryTime"
                )
                return getContainerStatus(projectId, pipelineId, buildId, vmSeqId, userId, name, retryTime - 1)
            } else {
                logger.error("[$buildId]|[$vmSeqId] containerName: $name getContainerStatus failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取容器状态接口超时, url: $url"
                )
            }
        }
    }

    fun getWorkspaceList() {

    }

    fun getTasks(
        projectId: String,
        pipelineId: String,
        staffName: String,
        taskId: String,
        retryFlag: Int = 3
    ): JSONObject {
        val url = devCloudUrl + "/api/v2.1/tasks/" + taskId
        val request = Request.Builder()
            .url(url)
            .headers(
                Headers.of(
                    SmartProxyUtil.makeHeaders(
                        devCloudAppId,
                        devCloudToken,
                        staffName,
                        smartProxyToken,
                        projectId,
                        pipelineId
                    )
                )
            )
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
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                                "获取TASK状态接口异常：http response code: ${response.code()}"
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
                return getTasks(projectId, pipelineId, staffName, taskId, retryFlag - 1)
            } else {
                logger.error("$taskId get task status failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取TASK状态接口超时, url: $url"
                )
            }
        }
    }

    /**
     * first： 成功or失败
     * second：成功时为containerName，失败时为错误信息
     */
    fun waitTaskFinish(
        userId: String,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): Triple<TaskStatus, String, ErrorCodeEnum> {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("Wait task: $taskId finish timeout(10min)")
                return Triple(TaskStatus.TIMEOUT, "创建容器超时（10min）", ErrorCodeEnum.CREATE_VM_ERROR)
            }
            Thread.sleep(1 * 1000)
            val (isFinish, success, msg, errorCodeEnum) = getTaskResult(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            )
            return when {
                !isFinish -> continue@loop
                !success -> {
                    Triple(TaskStatus.FAILED, msg, errorCodeEnum)
                }
                else -> Triple(TaskStatus.SUCCEEDED, msg, errorCodeEnum)
            }
        }
    }

    private fun getTaskResult(
        userId: String,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): TaskResult {
        try {
            val taskResponse = getTasks(projectId, pipelineId, userId, taskId)
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
}
data class TaskResult(
    val isFinish: Boolean,
    val success: Boolean,
    val msg: String,
    val errorCodeEnum: ErrorCodeEnum = ErrorCodeEnum.CREATE_VM_ERROR
)
