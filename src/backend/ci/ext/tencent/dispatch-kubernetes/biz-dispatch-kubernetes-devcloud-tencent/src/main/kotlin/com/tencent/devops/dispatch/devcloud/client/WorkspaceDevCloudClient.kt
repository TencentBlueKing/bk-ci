package com.tencent.devops.dispatch.devcloud.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.pojo.Environment
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentListReq
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentListRsp
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentOpRsp
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentOpRspData
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentStatus
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentStatusRsp
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatusRsp
import com.tencent.devops.dispatch.devcloud.pojo.UidReq
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.interfaces.CommonService
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class WorkspaceDevCloudClient @Autowired constructor(
    private val dslContext: DSLContext,
    private val commonService: CommonService,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao
) {
    private val logger = LoggerFactory.getLogger(WorkspaceDevCloudClient::class.java)

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.apiUrl}")
    val devCloudUrl: String = ""

    fun createWorkspace(userId: String, environment: Environment): EnvironmentOpRspData {
        val url = devCloudUrl + "/environment/create"
        val body = ObjectMapper().writeValueAsString(environment)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    devCloudAppId,
                    devCloudToken,
                    userId
                )
                    .toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body.toString()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("User $userId create environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 创建环境接口异常: ${response.code}"
                    )
                }

                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if ( HttpStatus.OK.value == environmentOpRsp.code) {
                    return environmentOpRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 创建环境接口返回失败: ${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 创建环境接口超时, url: $url"
            )
        }
    }

    fun operatorWorkspace(
        userId: String,
        environmentUid: String,
        workspaceName: String,
        environmentAction: EnvironmentAction
    ): EnvironmentOpRspData {
        val url = devCloudUrl + "/environment/${environmentAction.getValue()}"
        logger.info("User $userId request url: $url, enviromentUid: $environmentUid")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    devCloudAppId,
                    devCloudToken,
                    userId
                )
                    .toHeaders()
            )
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(UidReq(environmentUid))
                )
            )
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 操作环境接口异常：${response.code}"
                    )
                }
                logger.info("User $userId ${environmentAction.getValue()} environment response: $responseContent")
                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if (200 == environmentOpRsp.code) {
                    // 记录操作历史
                    dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        environmentUid = environmentUid,
                        operator = "admin",
                        action = environmentAction
                    )

                    return environmentOpRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 操作环境接口返回失败：${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId ${environmentAction.getValue()} environment get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 操作环境接口超时, url: $url"
            )
        }
    }

    fun getWorkspaceStatus(
        userId: String,
        environmentUid: String,
        retryTime: Int = 3
    ): EnvironmentStatus {
        val url = devCloudUrl + "/environment/status"
        logger.info("User $userId get environment status: $url")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    devCloudAppId,
                    devCloudToken,
                    userId
                )
                    .toHeaders()
            )
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(UidReq(environmentUid))
                )
            )
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("User $userId get environment status $environmentUid response: $responseContent")
                if (!response.isSuccessful && retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getWorkspaceStatus(userId, environmentUid, retryTimeLocal)
                }

                if (!response.isSuccessful && retryTime <= 0) {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 获取环境状态接口异常: ${response.code}"
                    )
                }

                val environmentStatusRsp: EnvironmentStatusRsp = jacksonObjectMapper().readValue(responseContent)
                if (200 == environmentStatusRsp.code) {
                    return environmentStatusRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 操作环境接口返回失败：${environmentStatusRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info(
                    "User $userId get environment status SocketTimeoutException. " +
                        "retry: $retryTime"
                )
                return getWorkspaceStatus(userId, environmentUid, retryTime - 1)
            } else {
                logger.error("User $userId get environment status failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取环境状态接口超时, url: $url"
                )
            }
        }
    }

    fun getWorkspaceList(
        userId: String,
        label: String,
        retryTime: Int = 3
    ): List<Environment> {
        val url = devCloudUrl + "/environment/query"
        logger.info("User $userId get environment list: $url")
        val body = ObjectMapper().writeValueAsString(EnvironmentListReq(userId, 0, 0))
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    devCloudAppId,
                    devCloudToken,
                    userId
                )
                    .toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("User $userId get environment list response: $responseContent")
                if (!response.isSuccessful && retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getWorkspaceList(userId, label, retryTimeLocal)
                }

                if (!response.isSuccessful && retryTime <= 0) {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 获取环境列表接口异常: ${response.code}"
                    )
                }
                val environmentListRsp: EnvironmentListRsp = jacksonObjectMapper().readValue(responseContent)
                if (200 == environmentListRsp.code) {
                    return environmentListRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage,
                        "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 操作环境列表返回失败：${environmentListRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info(
                    "User $userId get environment list SocketTimeoutException. " +
                        "retry: $retryTime"
                )
                return getWorkspaceList(userId, label, retryTime - 1)
            } else {
                logger.error("User $userId get environment list failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取环境列表接口超时, url: $url"
                )
            }
        }
    }

    fun getTasks(
        userId: String,
        taskUid: String,
        retryFlag: Int = 3
    ): TaskStatusRsp {
        val url = "$devCloudUrl/task/status?uid=$taskUid"
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    devCloudAppId,
                    devCloudToken,
                    userId
                )
                    .toHeaders()
            )
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("Get task status $taskUid failed, responseCode: ${response.code}")

                    // 接口请求失败时，sleep 5s，再查一次
                    Thread.sleep(5 * 1000)
                    return getTasks(userId, taskUid, retryFlag - 1)
                }

                logger.info("Get task status $taskUid response: $responseContent")
                return jacksonObjectMapper().readValue(responseContent)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryFlag > 0) {
                logger.info("$taskUid get task SocketTimeoutException. retry: $retryFlag")
                return getTasks(userId, taskUid, retryFlag - 1)
            } else {
                logger.error("$taskUid get task status failed.", e)
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
        taskId: String
    ): Triple<TaskStatusEnum, String, ErrorCodeEnum> {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("Wait task: $taskId finish timeout(10min)")
                return Triple(TaskStatusEnum.abort, "创建环境超时（10min）", ErrorCodeEnum.CREATE_VM_ERROR)
            }
            Thread.sleep(1 * 1000)
            val (isFinish, success, msg, errorCodeEnum) = getTaskResult(
                userId = userId,
                taskId = taskId
            )
            return when {
                !isFinish -> continue@loop
                !success -> {
                    Triple(TaskStatusEnum.failed, msg, errorCodeEnum)
                }
                else -> Triple(TaskStatusEnum.successed, msg, errorCodeEnum)
            }
        }
    }

    private fun getTaskResult(
        userId: String,
        taskId: String
    ): TaskResult {
        try {
            val taskResponse = getTasks(userId, taskId)
            val actionCode = taskResponse.code
            return if (200 != actionCode) {
                // 创建失败
                val msg = taskResponse.message
                logger.error("Execute task: $taskId failed, actionCode is $actionCode, msg: $msg")
                TaskResult(isFinish = true, success = false, msg = msg)
            } else {
                when (taskResponse.data.status) {
                    TaskStatusEnum.successed -> {
                        logger.info("Task: $taskId success taskResponse: $taskResponse")
                        TaskResult(isFinish = true, success = true, msg = "")
                    }
                    TaskStatusEnum.failed -> {
                        val resultDisplay = taskResponse.data.logs
                        logger.error("Task: $taskId failed, taskResponse: $taskResponse")
                        TaskResult(isFinish = true, success = false, msg = resultDisplay.toString() ?: "")
                    }
                    else -> TaskResult(isFinish = false, success = false, msg = "")
                }
            }
        } catch (e: Exception) {
            logger.error("Get dev cloud task error, taskId: $taskId", e)
            return TaskResult(isFinish = true, success = false, msg = "创建失败，异常信息:${e.message}")
        }
    }

    fun makeHeaders(
        appId: String,
        token: String,
        userId: String
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val timestampMillis = System.currentTimeMillis().toString()
        headerBuilder["X-Timestamp"] = timestampMillis
        headerBuilder["X-Staffname"] = userId
        val requestId = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["X-Reqeust-Id"] = requestId
        val encKey = ShaUtils.sha256("$appId,$timestampMillis,$userId,$requestId,$token")
        headerBuilder["Key"] = encKey

        return headerBuilder
    }
}
data class TaskResult(
    val isFinish: Boolean,
    val success: Boolean,
    val msg: String,
    val errorCodeEnum: ErrorCodeEnum = ErrorCodeEnum.CREATE_VM_ERROR
)
