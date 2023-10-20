package com.tencent.devops.environment.service.job.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.req.JobCloudPermission
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
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

        private const val QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&return_ip_result=%s"
        private const val GET_ACCOUNT_LIST_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&category=%s&account=%s&alias=%s&start=%s&length=%s"

        private val logger = LoggerFactory.getLogger(ApigwJobCloudApi::class.java)

        private val threadLocal = ThreadLocal<String>()
        fun set(value: String) {
            logger.debug("-----setThreadLocal------: $value")
            threadLocal.set(value)
        }

        fun get(): String? {
            logger.debug("-----getThreadLocal------: ${threadLocal.get()}")
            return threadLocal.get()
        }

        fun remove() {
            threadLocal.remove()
        }
    }

    private fun getAuthHeaderMap(bkAuthorization: String): MutableMap<String, String> {
        return mutableMapOf(
            "accept" to "*/*",
            "Content-Type" to "application/json",
            "X-Bkapi-Authorization" to bkAuthorization
        )
    }

    fun getJobCloudAuthReq(bkUsername: String): JobCloudAuthenticationReq {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"${bkUsername}\"}"

        val operationName = get()
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

    fun logWithLengthLimit(logOrigin: String): String {
        if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }

    fun <T : JobCloudPermission, U : Any> executePostRequest(bkUsername: String, jobCloud: T): JobResult<U> {
        val operationName = get()
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq(bkUsername)
        jobCloud.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloud.bkScopeId = jobCloudAuthenticationReq.bkScopeId
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val requestContent = jacksonObjectMapper().writeValueAsString(jobCloud)
        if (logger.isDebugEnabled)
            logger.debug(
                "[${operationName}] " +
                    "headers: $headers, " +
                    "jianingzhaotest1020-url: ${jobCloudAuthenticationReq.url}, " +
                    "body: $requestContent"
            )
        return getResultFromRes(OkhttpUtils.doPost(jobCloudAuthenticationReq.url, requestContent, headers))
    }

    fun <T : Any, U> executeGetRequest(bkUsername: String, vararg args: U): JobResult<T> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq(bkUsername)
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val suffix = when (get()) {
            "queryJobInstanceStatus" -> QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX
            "getAccountList" -> GET_ACCOUNT_LIST_URL_SUFFIX
            else -> ""
        }
        val url = jobCloudAuthenticationReq.url + String.format(
            suffix, jobCloudAuthenticationReq.bkScopeType, jobCloudAuthenticationReq.bkScopeId, *args
        )
        if (logger.isDebugEnabled)
            logger.debug("[${get()}] headers: ${logWithLengthLimit(headers.toString())}, url: $url")
        return getResultFromRes(OkhttpUtils.doGet(url, headers))
    }

    private fun <T> getResultFromRes(response: Response): JobResult<T> {
        val operationName = get()
        if (logger.isDebugEnabled) logger.debug("[getResultFromRes] operateName: $operationName")
//        remove()
        try {
            val responseBody = response.body?.string()
            if (logger.isDebugEnabled) {
                val responseLog = logWithLengthLimit(responseBody.toString())
                if (logger.isDebugEnabled) logger.debug("[$operationName] response body(origin): $responseLog")
            }

            val jobCloudResp = jacksonObjectMapper().readValue<JobCloudResp<T>>(responseBody!!)
            if (logger.isDebugEnabled)
                logger.debug("[$operationName] response body(deserialized JobCloudResp<T>): " +
                                 "${logWithLengthLimit(jobCloudResp.toString())}")
            if (!jobCloudResp.result) {
                logger.error(
                    "[$operationName] Execute failed! Req ID: ${jobCloudResp.jobRequestId}, " +
                        "Error code: ${jobCloudResp.code}, " +
                        "Error msg: ${jobCloudResp.message}"
                )
                throw RemoteServiceException(
                    "Execute failed! Req ID: ${jobCloudResp.jobRequestId}, " +
                        "Error code: ${jobCloudResp.code}, " +
                        "Error msg: ${jobCloudResp.message}"
                )
            } else {
                var jsonData = ""
                val operationResult: Any? =
                    if (null != jobCloudResp.data) {
                        jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                        jacksonObjectMapper().readValue(jsonData)
                    } else {
                        null
                    }
                if (logger.isDebugEnabled) {
                    logger.debug("[$operationName] jobCloudResp.data: " +
                                     "${logWithLengthLimit(jobCloudResp.data.toString())}")
                    logger.debug("[$operationName] serialized jsonData: ${logWithLengthLimit(jsonData)}")
                    logger.debug("[$operationName] {$operationName}Result: " +
                                     "${logWithLengthLimit(operationResult.toString())}")
                }
                return JobResult(
                    code = jobCloudResp.code,
                    result = jobCloudResp.result,
                    jobRequestId = jobCloudResp.jobRequestId,
                    data = operationResult
                ) as JobResult<T>
            }
        } catch (exception: Exception) {
            logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
            throw exception
        }
    }
}