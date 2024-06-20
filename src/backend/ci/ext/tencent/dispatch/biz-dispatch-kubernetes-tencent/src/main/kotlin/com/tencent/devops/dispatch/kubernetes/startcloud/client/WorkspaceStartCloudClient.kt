package com.tencent.devops.dispatch.kubernetes.startcloud.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.CgsQueryReq
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentDefaltRsp
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentDelete
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentResourceDataRsp
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentShare
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentShareRep
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentUnShare
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentUserCreate
import java.net.SocketTimeoutException
import java.util.UUID
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Suppress("ALL")
@Component
class WorkspaceStartCloudClient @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceStartCloudClient::class.java)
        const val OK = 0
        const val USER_ALREADY_EXISTED = 32001
        const val HAS_BEEN_DELETED = 32006
        const val APP_NOT_BIND_CGS = 32004
        const val NO_CGS_CHOOSE = 32005
        private const val DEFAULT_CGS_PER_PAGE = 10000
        private const val DEFAULT_CGS_PAGE = 0
    }

    @Value("\${startCloud.appId}")
    val appId: String = ""

    @Value("\${startCloud.appKey}")
    val appKey: String = ""

    @Value("\${startCloud.apiUrl}")
    val apiUrl: String = ""

    fun createUser(userId: String, environment: EnvironmentUserCreate): Boolean {
        val url = "$apiUrl/openapi/user/create"
        val body = JsonUtil.toJson(environment, false)
        val id = UUID.randomUUID()
        logger.info("$id|User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                makeHeaders(
                    body
                ).toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$id|User $userId create environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 创建user接口异常: ${response.code}"
                    )
                }

                val environmentRsp: EnvironmentDefaltRsp = jacksonObjectMapper().readValue(responseContent)
                when (environmentRsp.code) {
                    OK -> return true
                    USER_ALREADY_EXISTED -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        " 创建user接口返回失败:" +
                            "${environmentRsp.code}-${environmentRsp.message}"
                    )

                    else -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 创建user接口返回异常:" +
                            "${environmentRsp.code}-${environmentRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("$id|User $userId create environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 创建user接口超时, url: $url"
            )
        }
    }

    fun shareWorkspace(userId: String, environment: EnvironmentShare): String {
        val url = "$apiUrl/openapi/computer/share"
        val body = JsonUtil.toJson(environment, false)
        val id = UUID.randomUUID()
        logger.info("$id|User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                makeHeaders(
                    body
                ).toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$id|User $userId share environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 分享云桌面接口异常: ${response.code}"
                    )
                }

                val environmentRsp: EnvironmentShareRep = jacksonObjectMapper().readValue(responseContent)
                when (environmentRsp.code) {
                    OK -> return environmentRsp.data.resourceId
                    else -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 分享云桌面接口返回异常:" +
                            "${environmentRsp.code}-${environmentRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId share environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 分享云桌面接口超时, url: $url"
            )
        }
    }

    fun unShareWorkspace(userId: String, unShare: EnvironmentUnShare): Boolean {
        val url = "$apiUrl/openapi/computer/unshare"
        val body = JsonUtil.toJson(unShare, false)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                makeHeaders(
                    body
                ).toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("User $userId unShare environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 取消分享云桌面接口异常: ${response.code}"
                    )
                }

                val environmentRsp: EnvironmentDefaltRsp = jacksonObjectMapper().readValue(responseContent)
                when (environmentRsp.code) {
                    OK -> return true
                    else -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        " 取消分享云桌面接口返回异常:" +
                            "${environmentRsp.code}-${environmentRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId unShare environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 取消分享云桌面接口超时, url: $url"
            )
        }
    }

    fun deleteWorkspace(
        userId: String,
        workspaceName: String,
        environment: EnvironmentDelete
    ) {
        val url = "$apiUrl/openapi/computer/destroy"
        val body = JsonUtil.toJson(environment, false)
        logger.info("deleteWorkspace User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                makeHeaders(
                    body
                ).toHeaders()
            )
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
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
                        "${response.code}"
                    )
                }
                logger.info("User $userId  environment response: $responseContent")
                val environmentOpRsp: EnvironmentDefaltRsp = jacksonObjectMapper().readValue(responseContent)
                if (OK == environmentOpRsp.code || environmentOpRsp.code == HAS_BEEN_DELETED) {
                    // 记录操作历史
                    dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        environmentUid = "",
                        operator = userId,
                        uid = "",
                        action = EnvironmentAction.DELETE
                    )
                } else {
                    throw BuildFailureException(
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "${environmentOpRsp.code}-${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId environment get SocketTimeoutException.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.OP_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun queryCgsPageList(cgsQueryReq: CgsQueryReq): List<EnvironmentResourceData> {
        val url = "$apiUrl/openapi/cgs/list"
        val body = JsonUtil.toJson(cgsQueryReq, false)
        logger.info("getResourceList request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(
                makeHeaders(
                    body
                )
                    .toHeaders()
            )
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }

                val environmentRsp: EnvironmentResourceDataRsp = jacksonObjectMapper().readValue(responseContent)
                when {
                    OK == environmentRsp.code && environmentRsp.data != null
                    -> return environmentRsp.data.rows

                    else -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "(${environmentRsp.code}-${environmentRsp.message})"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("getResourceList SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun makeHeaders(
        body: String
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["x-start-appid"] = appId
        val timestampMillis = System.currentTimeMillis().toString().take(10)
        headerBuilder["x-start-timestamp"] = timestampMillis
        headerBuilder["x-start-signature"] = ShaUtils.sha256("$appId$appKey$timestampMillis$body").uppercase()

        return headerBuilder
    }
}
