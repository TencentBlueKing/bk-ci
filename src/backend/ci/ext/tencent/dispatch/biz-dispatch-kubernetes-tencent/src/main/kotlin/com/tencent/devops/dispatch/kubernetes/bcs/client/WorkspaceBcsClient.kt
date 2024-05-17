package com.tencent.devops.dispatch.kubernetes.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_CREATION_FAILED_EXCEPTION_INFORMATION
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
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
import com.tencent.devops.dispatch.kubernetes.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.bcs.pojo.UidReq
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmReq
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmResp
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient.Companion.APP_NOT_BIND_CGS
import com.tencent.devops.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient.Companion.NO_CGS_CHOOSE
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentCreate
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentCreateRsp
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentOperateRsp
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.ListCgsResp
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.ListCgsRespData
import com.tencent.devops.remotedev.pojo.image.ListVmImagesResp
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
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
import java.util.UUID

@Component
class WorkspaceBcsClient @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceBcsClient::class.java)
        const val OK = 0
    }

    @Value("\${bcsCloud.apiUrl}")
    val bcsCloudUrl: String = ""

    @Value("\${apigw.appCode}")
    val appCode: String = ""

    @Value("\${apigw.appToken}")
    val appToken: String = ""

    /**
     * TODO: 函数带有 start 的都是之前是放到 start client 下但是操作的是 bcs 的接口，先平移过来，未来看整合到一起
     */

    fun startCreateWorkspace(userId: String, environment: EnvironmentCreate): EnvironmentCreateRsp.EnvironmentCreateRspData {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/createvm"
        val id = UUID.randomUUID()
        val body = JsonUtil.toJson(environment, false)
        logger.info("$id|User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$id|User $userId create environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }

                val environmentRsp: EnvironmentCreateRsp = jacksonObjectMapper().readValue(responseContent)
                logger.info("$id|createWorkspace rsp: $environmentRsp")
                when {
                    WorkspaceStartCloudClient.OK == environmentRsp.code && environmentRsp.data != null
                    -> return environmentRsp.data

                    APP_NOT_BIND_CGS == environmentRsp.code || NO_CGS_CHOOSE == environmentRsp.code
                    -> throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        " ${environment.basicBody.zoneId}地区${environment.basicBody.machineType}" +
                                "型云桌面资源不足(${environmentRsp.code})"
                    )

                    else -> throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "(${environmentRsp.code}-${environmentRsp.message})"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun createWorkspace(userId: String, environment: Environment): EnvironmentOpRspData {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/create"
        val body = ObjectMapper().writeValueAsString(environment)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
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
                        "Env creation interface exception.: ${response.code}"
                    )
                }

                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK == environmentOpRsp.code) {
                    return environmentOpRsp.data
                } else {
                    val message = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        message,
                        "$message: ${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            val message = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = message,
                errorMessage = "$message, url: $url"
            )
        }
    }

    fun startOperateWorkspace(
        userId: String,
        action: EnvironmentAction,
        workspaceName: String,
        environmentOperate: EnvironmentOperate
    ): EnvironmentOperateRsp.EnvironmentOperateRspData {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/${action.action}"
        val body = JsonUtil.toJson(environmentOperate, false)
        logger.info("$userId ${action.action} workspace url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }
                logger.info("$userId ${action.action} workspace response: $responseContent")
                val environmentOpRsp: EnvironmentOperateRsp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == environmentOpRsp.code) {
                    // 记录操作历史
                    dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        environmentUid = environmentOperate.uid,
                        operator = userId,
                        uid = environmentOpRsp.data!!.taskUid,
                        action = action
                    )

                    return environmentOpRsp.data!!
                } else {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "${environmentOpRsp.code}-${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("$userId ${action.action} workspace get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun operatorWorkspace(
        userId: String,
        environmentUid: String,
        workspaceName: String,
        environmentAction: EnvironmentAction,
        envPatchStr: Map<String, String> = mutableMapOf()
    ): EnvironmentOpRspData {
        val url = bcsCloudUrl + "/api/v1/remotedevenv/${environmentAction.getValue()}"
        val body = JsonUtil.toJson(UidReq(uid = environmentUid, env = envPatchStr, deleteCbs = true))
        logger.info("User $userId request url: $url, enviromentUid: $environmentUid, " +
            "patchStr: $envPatchStr, body:$body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    val message = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage
                    throw BuildFailureException(
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        message,
                        "$message：${response.code}"
                    )
                }
                logger.info("User $userId ${environmentAction.getValue()} environment response: $responseContent")
                val environmentOpRsp: EnvironmentOpRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK != environmentOpRsp.code) {
                    throw BuildFailureException(
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
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
                    uid = environmentOpRsp.data.taskUid,
                    action = environmentAction
                )

                return environmentOpRsp.data
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId ${environmentAction.getValue()} environment get SocketTimeoutException.", e)
            val message = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = message,
                errorMessage = "$message, url: $url"
            )
        }
    }

    fun startGetWorkspaceInfo(
        userId: String,
        environmentOperate: EnvironmentOperate
    ): EnvironmentStatus {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/status"
        val body = JsonUtil.toJson(environmentOperate, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }
                logger.info("$userId get workspace info body: $body response: $responseContent")
                val environmentInfoRsp: EnvironmentStatusRsp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == environmentInfoRsp.code) {
                    return environmentInfoRsp.data!!
                } else {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "${environmentInfoRsp.code}-${environmentInfoRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("$userId get workspace info SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
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
            .url(url)
            .headers(makeHeaders().toHeaders())
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
                    val message = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        message,
                        "$message: ${response.code}"
                    )
                }

                val environmentStatusRsp: EnvironmentStatusRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK == environmentStatusRsp.code) {
                    return environmentStatusRsp.data!!
                } else {
                    val message = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
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
                    errorType = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
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
            .url(url)
            .headers(makeHeaders().toHeaders())
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
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                        "For third-party service DEVCLOUD exceptions, contact O2000 for troubleshooting and obtain " +
                                "the exception information - obtain the environment details: ${response.code}"
                    )
                }

                val environmentDetailRsp: EnvironmentDetailRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK == environmentDetailRsp.code) {
                    return environmentDetailRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
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
                    errorType = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
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
            .url(url)
            .headers(makeHeaders().toHeaders())
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
                    val message = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        message,
                        "$message: ${response.code}"
                    )
                }
                val environmentListRsp: EnvironmentListRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK == environmentListRsp.code) {
                    return environmentListRsp.data
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage,
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
                    errorType = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.ENVIRONMENT_LIST_INTERFACE_ERROR.formatErrorMessage,
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
            .url(url)
            .headers(makeHeaders().toHeaders())
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
                    third = ErrorCodeEnum.CREATE_ENV_ERROR
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
            return if (OK != actionCode) {
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

    fun startGetResourceVm(
        data: ResourceVmReq
    ): List<ResourceVmRespData>? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/resource/vm/list"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }
                logger.debug("get resource vm body: $body response: $responseContent")
                val resp: ResourceVmResp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == resp.code) {
                    return resp.data?.zoneResources
                } else {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get resource vm SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun startListCgs(): List<ListCgsRespData> {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/listcgs"
        val body = JsonUtil.toJson("", false)
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("get cgs list response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.formatErrorMessage,
                        " 获取listcgs接口异常: ${response.code}"
                    )
                }

                val resp: ListCgsResp = jacksonObjectMapper().readValue(responseContent)
                when (resp.code) {
                    WorkspaceStartCloudClient.OK -> {
                        if (resp.data == null) {
                            throw BuildFailureException(
                                com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorType,
                                com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorCode,
                                com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.formatErrorMessage,
                                " 获取listcgs接口异常: data is null"
                            )
                        }
                        return resp.data
                    }

                    else -> throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.formatErrorMessage,
                        " 获取listcgs接口异常: ${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get listcgs SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_CGS_ERROR.formatErrorMessage,
                errorMessage = " 获取listcgs接口超时, url: $url"
            )
        }
    }

    // 获取基础镜像列表
    fun startGetVmStandardImages(): List<StandardVmImage>? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/list/image"
        val body = JsonUtil.toJson("", false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.RESOURCE_VM_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }
                logger.debug("list vm image body: $body response: $responseContent")
                val resp: ListVmImagesResp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == resp.code) {
                    return resp.data
                } else {
                    throw BuildFailureException(
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.errorType,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.errorCode,
                        com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.formatErrorMessage,
                        "${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get resource vm SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.errorType,
                errorCode = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.errorCode,
                formatErrorMessage = com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum.LIST_IMAGE_INTERFACE_ERROR.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    private fun makeHeaders(): Map<String, String> {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appToken)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        return mapOf("X-Bkapi-Authorization" to headerStr)
    }
}
data class TaskResult(
    val isFinish: Boolean,
    val success: Boolean,
    val msg: String,
    val errorCodeEnum: ErrorCodeEnum = ErrorCodeEnum.CREATE_ENV_ERROR
)
