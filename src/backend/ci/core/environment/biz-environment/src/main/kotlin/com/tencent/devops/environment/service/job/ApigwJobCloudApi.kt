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

@Component("ApigwJobCloudApi")
class ApigwJobCloudApi {
    // @Value("\${auth.appCode:}")
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    // @Value("\${auth.appSecret:}")
    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String = ""

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String = ""

    @Value("\${job.bkScopeIdStag:#{null}}")
    val bkScopeIdStag: String = ""

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
        fun setThreadLocal(value: String) {
            threadLocal.set(value)
        }

        fun getThreadLocal(): String? {
            return threadLocal.get()
        }

        fun removeThreadLocal() {
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
        val operationName = getThreadLocal()
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
        val bkScopeId = when (operationName) {
            "executeScript", "distributeFile", "terminateTask",
            "queryJobInstanceStatus", "queryJobInstanceLogs" -> bkScopeId

//          "createAccount", "deleteAccount", "getAccountList" -> bkScopeIdStag
            else -> bkScopeIdStag
        }
        return JobCloudAuthenticationReq(
            url = url,
            bkAuthorization = bkAuthorization,
            bkScopeType = bkScopeType,
            bkScopeId = bkScopeId
        )
    }

    fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }

    fun <T : JobCloudPermission, U : Any> executePostRequest(
        bkUsername: String,
        jobCloud: T,
        classOfU: Class<U>
    ): JobResult<U> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq(bkUsername)
        jobCloud.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloud.bkScopeId = jobCloudAuthenticationReq.bkScopeId
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val requestContent = jacksonObjectMapper().writeValueAsString(jobCloud)
        if (logger.isDebugEnabled)
            logger.debug(
                "[${getThreadLocal()}] headers: $headers, url: ${jobCloudAuthenticationReq.url}, body: $requestContent"
            )
        return getResultFromRes(OkhttpUtils.doPost(jobCloudAuthenticationReq.url, requestContent, headers), classOfU)
    }

    fun <T, U> executeGetRequest(bkUsername: String, classOfT: Class<T>, vararg args: U): JobResult<T> {
        val operationName = getThreadLocal()
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq(bkUsername)
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val suffix = when (operationName) {
            "queryJobInstanceStatus" -> QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX
            "getAccountList" -> GET_ACCOUNT_LIST_URL_SUFFIX
            else -> ""
        }
        val url = jobCloudAuthenticationReq.url + String.format(
            suffix, jobCloudAuthenticationReq.bkScopeType, jobCloudAuthenticationReq.bkScopeId, *args
        )
        if (logger.isDebugEnabled)
            logger.debug("[$operationName] headers: ${logWithLengthLimit(headers.toString())}, url: $url")
        return getResultFromRes(OkhttpUtils.doGet(url, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): JobResult<T> {
        val operationName = getThreadLocal()
        removeThreadLocal()
        try {
            val responseBody = response.body?.string()
            if (logger.isDebugEnabled) {
                val responseLog = logWithLengthLimit(responseBody.toString())
                logger.debug("[$operationName] response body(origin): $responseLog")
            }

            val jobCloudResp = jacksonObjectMapper().readValue<JobCloudResp<T>>(responseBody!!)
            if (logger.isDebugEnabled)
                logger.debug(
                    "[$operationName] response body(deserialized JobCloudResp<T>): " +
                        logWithLengthLimit(jobCloudResp.toString())
                )
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
                val operationResult: T? =
                    if (null != jobCloudResp.data) {
                        jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                        jacksonObjectMapper().readValue(jsonData, classOfT)
                    } else {
                        null
                    }
                if (logger.isDebugEnabled) {
                    logger.debug("[$operationName] operationResult type: " + operationResult!!::class)
                    logger.debug("[$operationName] serialized jsonData: ${logWithLengthLimit(jsonData)}")
                    logger.debug(
                        "[$operationName] ${operationName}Result: " +
                            logWithLengthLimit(operationResult.toString())
                    )
                }
                val jobResult = JobResult(
                    code = jobCloudResp.code,
                    result = jobCloudResp.result,
                    jobRequestId = jobCloudResp.jobRequestId,
                    data = operationResult
                )
                if (logger.isDebugEnabled)
                    logger.debug("[$operationName] jobResult1: " + logWithLengthLimit(jobResult.toString()))
                return jobResult
            }
        } catch (exception: Exception) {
            logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
            throw exception
        }
    }
}