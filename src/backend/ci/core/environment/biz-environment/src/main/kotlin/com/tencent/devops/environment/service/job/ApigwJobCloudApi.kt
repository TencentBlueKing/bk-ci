package com.tencent.devops.environment.service.job.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudResult
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.jobcloudreq.JobCloudPermission
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudResp
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component("ApigwJobCloudApi")
class ApigwJobCloudApi {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String = ""

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String = ""

    @Value("\${job.jobCloudApiBaseUrl:#{null}}")
    val jobCloudApiBaseUrl: String? = null

    @Value("\${job.executeScriptPath:#{\"/api/v3/fast_execute_script\"}}")
    val executeScriptPath: String = ""

    @Value("\${job.distributeFilePath:#{\"/api/v3/fast_transfer_file\"}}")
    val distributeFilePath: String = ""

    @Value("\${job.terminateTaskPath:#{\"/api/v3/operate_job_instance\"}}")
    val terminateTaskPath: String = ""

    @Value("\${job.queryJobInstanceStatusPath:#{\"/api/v3/get_job_instance_status\"}}")
    val queryJobInstanceStatusPath: String = ""

    @Value("\${job.queryJobInstanceLogsPath:#{\"/api/v3/batch_get_job_instance_ip_log\"}}")
    val queryJobInstanceLogsPath: String = ""

    @Value("\${job.createAccountPath:#{\"/api/v3/create_account\"}}")
    val createAccountPath: String = ""

    @Value("\${job.deleteAccountPath:#{\"/api/v3/delete_account\"}}")
    val deleteAccountPath: String = ""

    @Value("\${job.getAccountListPath:#{\"/api/v3/get_account_list\"}}")
    val getAccountListPath: String = ""

    @Value("\${job.getStepInstanceDetailPath:#{\"/api/v3/get_step_instance_detail\"}}")
    val getStepInstanceDetailPath: String = ""

    @Value("\${job.getStepInstanceStatusPath:#{\"/api/v3/get_step_instance_status\"}}")
    val getStepInstanceStatusPath: String = ""

    @Value("\${job.queryAgentInfoPath:#{\"/api/v3/query_agent_info\"}}")
    val queryAgentInfoPath: String = ""

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        private const val QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&return_ip_result=%s"
        private const val GET_ACCOUNT_LIST_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&category=%s&account=%s&alias=%s&start=%s&length=%s"
        private const val GET_STEP_INSTANCE_DETAIL =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&step_instance_id=%s"
        private const val GET_STEP_INSTANCE_STATUS =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&step_instance_id=%s&execute_count=%s" +
                "&batch=%s&max_host_num_per_group=%s&keyword=%s&search_ip=%s&status=%s&tag=%s"

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

    private fun getJobCloudAuthReq(): JobCloudAuthenticationReq {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"$AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE\"}"
        val operationName = getThreadLocal()
        if (logger.isDebugEnabled) logger.debug("[getJobCloudAuthReq] operationName: $operationName")
        val url = jobCloudApiBaseUrl + when (operationName) {
            "executeScript" -> executeScriptPath
            "distributeFile" -> distributeFilePath
            "terminateTask" -> terminateTaskPath
            "queryJobInstanceStatus" -> queryJobInstanceStatusPath
            "queryJobInstanceLogs" -> queryJobInstanceLogsPath
            "createAccount" -> createAccountPath
            "deleteAccount" -> deleteAccountPath
            "getAccountList" -> getAccountListPath
            "getStepInstanceDetail" -> getStepInstanceDetailPath
            "getStepInstanceStatus" -> getStepInstanceStatusPath
            "queryAgentStatusFromJob" -> queryAgentInfoPath
            else -> ""
        }
        if (logger.isDebugEnabled) logger.debug("[getJobCloudAuthReq] url: $url")
        val bkScopeId = bkScopeId
        return JobCloudAuthenticationReq(
            url = url,
            bkAuthorization = bkAuthorization,
            bkScopeType = bkScopeType,
            bkScopeId = bkScopeId
        )
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }

    fun <T : JobCloudPermission, U : Any> executePostRequest(jobCloud: T, classOfU: Class<U>): JobCloudResult<U> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq()
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

    fun <T, U> executeGetRequest(classOfT: Class<T>, vararg args: U): JobCloudResult<T> {
        val operationName = getThreadLocal()
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq()
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val suffix = when (operationName) {
            "queryJobInstanceStatus" -> QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX
            "getAccountList" -> GET_ACCOUNT_LIST_URL_SUFFIX
            "getStepInstanceDetail" -> GET_STEP_INSTANCE_DETAIL
            "getStepInstanceStatus" -> GET_STEP_INSTANCE_STATUS
            else -> ""
        }
        val url = jobCloudAuthenticationReq.url + String.format(
            suffix, jobCloudAuthenticationReq.bkScopeType, jobCloudAuthenticationReq.bkScopeId, *args
        )
        if (logger.isDebugEnabled)
            logger.debug("[$operationName] headers: ${logWithLengthLimit(headers.toString())}, url: $url")
        return getResultFromRes(OkhttpUtils.doGet(url, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): JobCloudResult<T> {
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
                val jobCloudResult = JobCloudResult(
                    code = jobCloudResp.code,
                    result = jobCloudResp.result,
                    jobRequestId = jobCloudResp.jobRequestId,
                    data = operationResult
                )
                if (logger.isDebugEnabled)
                    logger.debug("[$operationName] jobCloudResult: " + logWithLengthLimit(jobCloudResult.toString()))
                return jobCloudResult
            }
        } catch (exception: Exception) {
            logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
            throw exception
        }
    }
}