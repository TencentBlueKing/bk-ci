package com.tencent.devops.dispatch.startCloud.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.startCloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.startCloud.pojo.CgsQueryReq
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentCreate
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentCreateRsp
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentDefaltRsp
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentDelete
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentResourceDataRsp
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentUserCreate
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentUnShare
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentShare
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentShareRep
import com.tencent.devops.dispatch.startCloud.pojo.Page
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

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
        private const val DEFAULT_CGS_PER_PAGE = 500
        private const val DEFAULT_CGS_PAGE = 0
    }

    @Value("\${startCloud.appId}")
    val appId: String = ""

    @Value("\${startCloud.appKey}")
    val appKey: String = ""

    @Value("\${startCloud.apiUrl}")
    val apiUrl: String = ""

    @Value("\${startCloud.appName}")
    val appName: String = "IEG_BKCI"

    fun createWorkspace(userId: String, environment: EnvironmentCreate): EnvironmentCreateRsp.EnvironmentCreateRspData {
        val url = "$apiUrl/openapi/computer/create"
        val body = JsonUtil.toJson(environment, false)
        logger.info("User $userId request url: $url, body: $body")
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
                logger.info("User $userId create environment response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }

                val environmentRsp: EnvironmentCreateRsp = jacksonObjectMapper().readValue(responseContent)
                logger.info("createWorkspace rsp: $environmentRsp")
                when {
                    OK == environmentRsp.code && environmentRsp.data != null && !environmentRsp.data.existed
                    -> return environmentRsp.data

                    OK == environmentRsp.code && environmentRsp.data != null && environmentRsp.data.existed
                    -> throw BuildFailureException(
                        ErrorCodeEnum.CLOUD_DESKTOP_EXIST.errorType,
                        ErrorCodeEnum.CLOUD_DESKTOP_EXIST.errorCode,
                        ErrorCodeEnum.CLOUD_DESKTOP_EXIST.formatErrorMessage,
                        environmentRsp.data.cgsIp
                    )

                    APP_NOT_BIND_CGS == environmentRsp.code || NO_CGS_CHOOSE == environmentRsp.code
                    -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        " ${environment.zoneId}地区${environment.machineType}型云桌面资源不足(${environmentRsp.code})"
                    )

                    else -> throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                        "(${environmentRsp.code}-${environmentRsp.message})"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                errorCode = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                formatErrorMessage = ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                errorMessage = " 接口超时, url: $url"
            )
        }
    }

    fun createUser(userId: String, environment: EnvironmentUserCreate): Boolean {
        val url = "$apiUrl/openapi/user/create"
        val body = JsonUtil.toJson(environment, false)
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
                logger.info("User $userId create environment response: ${response.code} || $responseContent")
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
            logger.error("User $userId create environment get SocketTimeoutException", e)
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
                logger.info("User $userId share environment response: ${response.code} || $responseContent")
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

    fun getResourceList(): List<EnvironmentResourceData> {
        var cgsPage = DEFAULT_CGS_PAGE
        val cgsData = mutableListOf<EnvironmentResourceData>()
        run outside@{
            while (true) {
                val request = CgsQueryReq(
                    appName = appName,
                    query = null,
                    page = Page(
                        start = cgsPage,
                        limit = DEFAULT_CGS_PER_PAGE,
                        sort = null
                    )
                )
                val cgsPageList = queryCgsPageList(request)
                cgsData.addAll(cgsPageList)
                if (cgsPageList.size < DEFAULT_CGS_PER_PAGE) {
                    return@outside
                }
                cgsPage++
            }
        }
        return cgsData
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
                logger.info("getResourceList response: ${response.code} || $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorType,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode,
                        ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.formatErrorMessage,
                        "${response.code}"
                    )
                }

                val environmentRsp: EnvironmentResourceDataRsp = jacksonObjectMapper().readValue(responseContent)
                logger.info("createWorkspace rsp: $environmentRsp")
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
