package com.tencent.devops.environment.service.job.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.req.JobCloudPermission
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.service.job.AuthenticationService
import com.tencent.devops.environment.service.job.ScriptExecuteService
import com.tencent.devops.environment.utils.job.NetworkUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import sun.java2d.StateTrackableDelegate.createInstance

class ApigwJobCloudApi {
    // @Value("\${auth.appCode:}")
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    // @Value("\${auth.appSecret:}")
    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String? = null

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String? = null

    @Value("\${job.bkScopeIdStag:#{null}}")
    val bkScopeIdStag: String? = null

    @Value("\${job.jobCloudProdUrlPrefix:#{null}}")
    val jobCloudProdUrlPrefix: String? = null

    @Value("\${job.jobCloudStagUrlPrefix:#{null}}")
    val jobCloudStagUrlPrefix: String? = null

    @Value("\${job.executeScriptPath:#{null}}")
    val executeScriptPath: String? = null

    @Value("\${job.distributeFilePath:#{null}}")
    val distributeFilePath: String? = null

    @Value("\${job.terminateTaskPath:#{null}}")
    val terminateTaskPath: String? = null

    @Value("\${job.queryJobInstanceStatusPath:#{null}}")
    val queryJobInstanceStatusPath: String? = null

    @Value("\${job.queryJobInstanceLogsPath:#{null}}")
    val queryJobInstanceLogsPath: String? = null

    @Value("\${job.createAccountPath:#{null}}")
    val createAccountPath: String? = null

    @Value("\${job.deleteAccountPath:#{null}}")
    val deleteAccountPath: String? = null

    @Value("\${job.getAccountListPath:#{null}}")
    val getAccountListPath: String? = null

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000
        private val logger = LoggerFactory.getLogger(ApigwJobCloudApi::class.java)

        private val threadLocal = ThreadLocal<String>()
        fun set(value: String) {
            threadLocal.set(value)
        }

        fun get(): String? {
            return threadLocal.get()
        }

        fun remove() {
            threadLocal.remove()
        }
    }

    fun getJobCloudAuthReq(bkUsername: String): JobCloudAuthenticationReq {
        val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"${bkUsername}\"}"

        val operationName = AuthenticationService.get()
        if (logger.isDebugEnabled) logger.debug("[appAuthentication] operationName: $operationName")
        val url = when (operationName) {
            "executeScript" -> jobCloudProdUrlPrefix + executeScriptPath
            "distributeFile" -> jobCloudProdUrlPrefix + distributeFilePath
            "terminateTask" -> jobCloudProdUrlPrefix + terminateTaskPath
            "queryJobInstanceStatus" -> jobCloudProdUrlPrefix + queryJobInstanceStatusPath
            "queryJobInstanceLogs" -> jobCloudProdUrlPrefix + queryJobInstanceLogsPath
            "createAccount" -> jobCloudStagUrlPrefix + createAccountPath
            "deleteAccount" -> jobCloudStagUrlPrefix + deleteAccountPath
            "getAccountList" -> jobCloudStagUrlPrefix + getAccountListPath
            else -> ""
        }
        if (logger.isDebugEnabled) logger.debug("[appAuthentication] url: $url")
        return JobCloudAuthenticationReq(
            url = url,
            bkAuthorization = bkAuthorization,
            bkScopeType = bkScopeType ?: "",
            bkScopeId = when (operationName) {
                "executeScript" -> bkScopeId
                "distributeFile" -> bkScopeId
                "terminateTask" -> bkScopeId
                "queryJobInstanceStatus" -> bkScopeId
                "queryJobInstanceLogs" -> bkScopeId
                "createAccount" -> bkScopeIdStag
                "deleteAccount" -> bkScopeIdStag
                "getAccountList" -> bkScopeIdStag
                else -> ""
            } ?: ""
        )
    }

    fun executeGetRequest(url: String): Response {
        TODO("Not yet vimplemented")
    }

    fun <T : JobCloudPermission, U : Any> executePostRequest(bkUsername: String, jobCloud: T): JobResult<U>? {
        val operationName = get()

        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq(bkUsername)
        jobCloud.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloud.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<Any> =
            this.executeHttpRequest(
                httpType = "post",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloud
            )

        var jsonData = ""
        val operationResult: Any =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                return null
            }
        if (logger.isDebugEnabled) {
            logger.debug("[$operationName] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[$operationName] serialized jsonData: $jsonData")
            logger.debug("[$operationName] {$operationName}Result: $operationResult")
        }

        return JobResult(
            code = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = operationResult
        ) as JobResult<U>
    }

    fun <T, U : Any> executeHttpRequest(
        httpType: String,
        url: String,
        bkAuthorization: String,
        jobCloudReq: T?
    ): JobCloudResp<U> {
        when (httpType) {
            "post" -> {
                val request = createPostRequest(url, bkAuthorization, jobCloudReq)
                return executeHttpRequest(request)
            }

            "get" -> {
                val request = createGetRequest(url, bkAuthorization)
                return executeHttpRequest(request)
            }

            else -> {
                logger.error("[executeHttpRequest] Invalid http type.")
                throw IllegalArgumentException("Invalid http request type")
            }
        }
    }

    private fun <T> createPostRequest(url: String, bkAuthorization: String, jobCloudReq: T?): Request {
        val requestContent = jacksonObjectMapper().writeValueAsString(jobCloudReq)
        val requestBody = RequestBody.create(
            "application/json;charset=utf-8".toMediaTypeOrNull(),
            requestContent
        )
        if (logger.isDebugEnabled) logger.debug("[createPostRequest] request body(serialized): $requestContent")
        return Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Bkapi-Authorization", bkAuthorization)
            .build()
    }

    private fun createGetRequest(url: String, bkAuthorization: String): Request {
        return Request.Builder()
            .url(url)
            .addHeader("X-Bkapi-Authorization", bkAuthorization)
            .get()
            .build()
    }

    private fun <T> executeHttpRequest(request: Request): JobCloudResp<T> {
        val operateName = AuthenticationService.get()
        if (logger.isDebugEnabled) logger.debug("[executeHttpRequest] operateName: $operateName")
        AuthenticationService.remove()
        com.tencent.devops.common.api.util.OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
                if (logger.isDebugEnabled) {
                    val requestLog =
                        if (request.toString().length > LOG_OUTPUT_MAX_LENGTH)
                            request.toString().substring(0, LOG_OUTPUT_MAX_LENGTH)
                        else
                            request.toString()
                    val responseLog =
                        if (responseBody.toString().length > LOG_OUTPUT_MAX_LENGTH)
                            responseBody.toString().substring(0, LOG_OUTPUT_MAX_LENGTH)
                        else
                            responseBody.toString()
                    if (logger.isDebugEnabled) {
                        logger.debug("[$operateName] request method/url/headers: $requestLog")
                        logger.debug("[$operateName] response body(origin): $responseLog")
                    }
                }

                val deserializedRespBody = jacksonObjectMapper().readValue<JobCloudResp<T>>(responseBody!!)
                if (logger.isDebugEnabled)
                    logger.debug("[$operateName] response body(deserialized JobCloudResp<T>): $deserializedRespBody")

                if (!deserializedRespBody.result) {
                    logger.error(
                        "[$operateName] Execute failed! Req ID: ${deserializedRespBody.jobRequestId}, " +
                            "Error code: ${deserializedRespBody.code}, " +
                            "Error msg: ${deserializedRespBody.message}"
                    )
                    throw RemoteServiceException(
                        "Execute failed! Req ID: ${deserializedRespBody.jobRequestId}, " +
                            "Error code: ${deserializedRespBody.code}, " +
                            "Error msg: ${deserializedRespBody.message}"
                    )
                }
                return deserializedRespBody
            } catch (exception: Exception) {
                logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
                throw exception
            }
        }
    }
}