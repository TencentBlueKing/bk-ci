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
    @Value("\${environment.apigw.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${environment.apigw.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${environment.cc.bkScopeType:#{null}}")
    val bkScopeType: String = ""

    @Value("\${environment.cc.bkScopeId:#{null}}")
    val bkScopeId: String = ""

    @Value("\${environment.job.jobCloudApiBaseUrl:#{null}}")
    val jobCloudApiBaseUrl: String? = null

    companion object {
        private const val LOG_OUTPUT_MAX_LENGTH = 4000

        private val postPathMap = mapOf(
            "executeScript" to "/api/v3/fast_execute_script",
            "distributeFile" to "/api/v3/fast_transfer_file",
            "terminateTask" to "/api/v3/operate_job_instance",
            "queryJobInstanceStatus" to "/api/v3/get_job_instance_status",
            "queryJobInstanceLogs" to "/api/v3/batch_get_job_instance_ip_log",
            "createAccount" to "/api/v3/create_account",
            "deleteAccount" to "/api/v3/delete_account",
            "getAccountList" to "/api/v3/get_account_list",
            "getStepInstanceDetail" to "/api/v3/get_step_instance_detail",
            "getStepInstanceStatus" to "/api/v3/get_step_instance_status",
            "queryAgentStatusFromJob" to "/api/v3/query_agent_info"
        )
        private val suffix = mapOf(
            "queryJobInstanceStatus" to "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&return_ip_result=%s",
            "getAccountList" to "/?bk_scope_type=%s&bk_scope_id=%s&category=%s&account=%s&alias=%s&start=%s&length=%s",
            "getStepInstanceDetail" to "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&step_instance_id=%s",
            "getStepInstanceStatus" to "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&step_instance_id=%s" +
                "&execute_count=%s&batch=%s&max_host_num_per_group=%s&keyword=%s&search_ip=%s&status=%s&tag=%s"
        )

        private val logger = LoggerFactory.getLogger(ApigwJobCloudApi::class.java)

        private val jobOperationName = ThreadLocal<String>()

        fun setJobOperationName(value: String) {
            jobOperationName.set(value)
        }

        fun getJobOperationName(): String? {
            return jobOperationName.get()
        }

        fun removeJobOperationName() {
            jobOperationName.remove()
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
        val operationName = getJobOperationName()
        if (logger.isDebugEnabled) logger.debug("[getJobCloudAuthReq] operationName: $operationName")
        val url = jobCloudApiBaseUrl + postPathMap[operationName]
        if (logger.isDebugEnabled) logger.debug("[getJobCloudAuthReq] url: $url")
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
                "[${getJobOperationName()}]url: ${jobCloudAuthenticationReq.url}, body: $requestContent"
            )
        logger.info("[${getJobOperationName()}]POST url: ${jobCloudAuthenticationReq.url}, " +
                        "body: ${logWithLengthLimit(requestContent)}")
        return getResultFromRes(OkhttpUtils.doPost(jobCloudAuthenticationReq.url, requestContent, headers), classOfU)
    }

    fun <T, U> executeGetRequest(classOfT: Class<T>, vararg args: U): JobCloudResult<T> {
        val operationName = getJobOperationName()
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq()
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val url = jobCloudAuthenticationReq.url + String.format(
            suffix[operationName] ?: "",
            jobCloudAuthenticationReq.bkScopeType,
            jobCloudAuthenticationReq.bkScopeId,
            *args
        )
        if (logger.isDebugEnabled)
            logger.debug("[$operationName] headers: ${logWithLengthLimit(headers.toString())}, url: $url")
        logger.info("[$operationName]GET url: $url")
        return getResultFromRes(OkhttpUtils.doGet(url, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): JobCloudResult<T> {
        val operationName = getJobOperationName()
        removeJobOperationName()
        try {
            val responseBody = response.body?.string()
            val responseLog = logWithLengthLimit(responseBody.toString())
            if (logger.isDebugEnabled) {
                logger.debug("[$operationName] response body(origin): $responseLog")
            }
            logger.info("[$operationName] response body(origin): $responseLog")
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