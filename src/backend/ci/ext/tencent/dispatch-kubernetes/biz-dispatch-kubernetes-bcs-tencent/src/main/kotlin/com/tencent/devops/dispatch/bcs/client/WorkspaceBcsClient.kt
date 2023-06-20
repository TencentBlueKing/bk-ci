package com.tencent.devops.dispatch.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_CREATION_FAILED_EXCEPTION_INFORMATION
import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.interfaces.CommonService
import com.tencent.devops.dispatch.kubernetes.pojo.BK_CREATE_ENV_TIMEOUT
import com.tencent.devops.dispatch.kubernetes.pojo.Environment
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentDetailRsp
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentListReq
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentListRsp
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentOpRsp
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentOpRspData
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentStatus
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentStatusRsp
import com.tencent.devops.dispatch.kubernetes.pojo.TaskStatusRsp
import com.tencent.devops.dispatch.kubernetes.pojo.UidReq
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import java.net.SocketTimeoutException
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WorkspaceBcsClient @Autowired constructor(
    private val dslContext: DSLContext,
    private val commonService: CommonService,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceBcsClient::class.java)
        private const val BCS_TOKEN_KEY = "BK-Devops-Token"
    }
    @Value("\${bcsCloud.appId}")
    val bcsCloudAppId: String = ""

    @Value("\${bcsCloud.token}")
    val bcsCloudToken: String = ""

    @Value("\${bcsCloud.apiUrl}")
    val bcsCloudUrl: String = ""

    fun createWorkspace(userId: String, environment: Environment): EnvironmentOpRspData {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/create"
        val body = ObjectMapper().writeValueAsString(environment)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    bcsCloudToken
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
                        ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_ERROR.getErrorMessage(),
                        "Env creation interface exception.: ${response.code}"
                    )
                }

                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if (HttpStatus.OK.value == environmentOpRsp.code) {
                    return environmentOpRsp.data
                } else {
                    val message = ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        message,
                        "$message: ${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            val message = ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.getErrorMessage()
            throw BuildFailureException(
                errorType = ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = message,
                errorMessage = "$message, url: $url"
            )
        }
    }

    fun operatorWorkspace(
        userId: String,
        environmentUid: String,
        workspaceName: String,
        environmentAction: EnvironmentAction,
        envPatchStr: String = ""
    ): EnvironmentOpRspData {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/${environmentAction.getValue()}"

        logger.info("User $userId request url: $url, enviromentUid: $environmentUid, patchStr: $envPatchStr")
        val body = JsonUtil.toJson(UidReq(uid = environmentUid, patch = envPatchStr))

        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(makeHeaders(bcsCloudToken).toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    val message = ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_ERROR.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        message,
                        "$message：${response.code}"
                    )
                }
                logger.info("User $userId ${environmentAction.getValue()} environment response: $responseContent")
                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if (HttpStatus.OK.value != environmentOpRsp.code) {
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.getErrorMessage(),
                        "For third-party service - DEVCLOUD exceptions, please contact O2000 for troubleshooting," +
                                " exception information - operation environment interface returns failure" +
                                "：${environmentOpRsp.message}"
                    )
                }

                // 记录操作历史
                dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspaceName,
                    environmentUid = environmentUid,
                    operator = "admin",
                    action = environmentAction
                )

                return environmentOpRsp.data
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId ${environmentAction.getValue()} environment get SocketTimeoutException.", e)
            val message = ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.getErrorMessage()
            throw BuildFailureException(
                errorType = ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = message,
                errorMessage = "$message, url: $url"
            )
        }
    }

    fun getWorkspaceStatus(
        userId: String,
        environmentUid: String,
        retryTime: Int = 3
    ): EnvironmentStatus {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/status"
        logger.info("User $userId get environment status: $url")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    bcsCloudToken
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
                    val message = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        message,
                        "$message: ${response.code}"
                    )
                }

                val environmentStatusRsp: EnvironmentStatusRsp = jacksonObjectMapper().readValue(responseContent)
                if (HttpStatus.OK.value == environmentStatusRsp.code) {
                    return environmentStatusRsp.data
                } else {
                    val message = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        message,
                        "$message：${environmentStatusRsp.message}"
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
                    errorType = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "Get the environment status interface timeout, url: $url"
                )
            }
        }
    }

    fun getWorkspaceDetail(
        userId: String,
        environmentUid: String,
        retryTime: Int = 3
    ): Environment {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/status"
        logger.info("User $userId get environment detail: $url")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    bcsCloudToken
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
                logger.info("User $userId get environment detail $environmentUid response: $responseContent")
                if (!response.isSuccessful && retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return getWorkspaceDetail(userId, environmentUid, retryTimeLocal)
                }

                if (!response.isSuccessful && retryTime <= 0) {
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage(),
                        "For third-party service DEVCLOUD exceptions, contact O2000 for troubleshooting and obtain " +
                                "the exception information - obtain the environment details: ${response.code}"
                    )
                }

                val environmentDetailRsp: EnvironmentDetailRsp = jacksonObjectMapper().readValue(responseContent)
                if (HttpStatus.OK.value == environmentDetailRsp.code) {
                    return environmentDetailRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage(),
                        "If the third-party service - DEVCLOUD is abnormal, please contact O2000 for troubleshooting," +
                                " and the exception information - operation environment details " +
                                "fails：${environmentDetailRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info(
                    "User $userId get environment detail SocketTimeoutException. " +
                        "retry: $retryTime"
                )
                return getWorkspaceDetail(userId, environmentUid, retryTime - 1)
            } else {
                logger.error("User $userId get environment detail failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "Get the environment details interface timeout, url: $url"
                )
            }
        }
    }

    fun getWorkspaceList(
        userId: String,
        label: String,
        retryTime: Int = 3
    ): List<Environment> {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/list"
        logger.info("User $userId get environment list: $url")
        val body = ObjectMapper().writeValueAsString(EnvironmentListReq(userId, 0, 0))
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    bcsCloudToken
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
                    val message = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.getErrorMessage()
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        message,
                        "$message: ${response.code}"
                    )
                }
                val environmentListRsp: EnvironmentListRsp = jacksonObjectMapper().readValue(responseContent)
                if (HttpStatus.OK.value == environmentListRsp.code) {
                    return environmentListRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.getErrorMessage(),
                        " list of operating environments returns a failure：${environmentListRsp.message}"
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
                    errorType = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "Get the list of environments interface timed out, url: $url"
                )
            }
        }
    }

    fun getTasks(
        userId: String,
        taskUid: String,
        retryFlag: Int = 3
    ): TaskStatusRsp {
        val url = "$bcsCloudUrl/api/v1/devops/task/status?uid=$taskUid"
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .headers(
                makeHeaders(
                    bcsCloudToken
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
                    errorType = ErrorCodeEnum.DEVCLOUD_TASK_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.DEVCLOUD_TASK_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.DEVCLOUD_TASK_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "Gets the TASK status interface timeout, url: $url"
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
                return Triple(
                    first = TaskStatusEnum.abort,
                    second = I18nUtil.getCodeLanMessage(BK_CREATE_ENV_TIMEOUT),
                    third = ErrorCodeEnum.DEVCLOUD_CREATE_VM_ERROR
                )
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
            return if (HttpStatus.OK.value != actionCode) {
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
            return TaskResult(
                isFinish = true,
                success = false,
                msg = "${I18nUtil.getCodeLanMessage(BK_CREATION_FAILED_EXCEPTION_INFORMATION)}:${e.message}"
            )
        }
    }

    fun makeHeaders(
        token: String
    ): Map<String, String> {
        return mapOf(BCS_TOKEN_KEY to token)
    }
}
data class TaskResult(
    val isFinish: Boolean,
    val success: Boolean,
    val msg: String,
    val errorCodeEnum: ErrorCodeEnum = ErrorCodeEnum.DEVCLOUD_CREATE_VM_ERROR
)
