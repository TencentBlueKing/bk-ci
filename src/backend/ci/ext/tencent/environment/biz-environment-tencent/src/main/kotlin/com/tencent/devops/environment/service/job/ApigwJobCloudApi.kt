/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.CustomException
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
        private const val INTERNAL_SERVER_ERROR_CODE = 1240002

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

        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    fun <T : JobCloudPermission, U : Any> executePostRequest(jobCloud: T, classOfU: Class<U>): JobCloudResult<U> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = getJobCloudAuthReq()
        jobCloud.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloud.bkScopeId = jobCloudAuthenticationReq.bkScopeId
        val headers = getAuthHeaderMap(jobCloudAuthenticationReq.bkAuthorization)
        val requestContent = mapper.writeValueAsString(jobCloud)
        logger.info(
            "[${getJobOperationName()}]POST url: ${jobCloudAuthenticationReq.url}, " +
                "body: ${logWithLengthLimit(requestContent)}"
        )
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
        logger.info("[$operationName]GET url: $url")
        return getResultFromRes(OkhttpUtils.doGet(url, headers), classOfT)
    }

    private fun <T> getResultFromRes(response: Response, classOfT: Class<T>): JobCloudResult<T> {
        val operationName = getJobOperationName()
        removeJobOperationName()
        try {
            val responseBody = response.body?.string()
            logger.info("[$operationName] response body(origin): ${logWithLengthLimit(responseBody.toString())}")
            val jobCloudResp = mapper.readValue<JobCloudResp<T>>(responseBody!!)
            if (!jobCloudResp.result) {
                val errorMsg = "Execute failed! Req ID: ${jobCloudResp.jobRequestId}, " +
                    "Error code: ${jobCloudResp.code}, " +
                    "Error msg: ${jobCloudResp.message}"
                logger.error("[$operationName] $errorMsg")
                if (INTERNAL_SERVER_ERROR_CODE == jobCloudResp.code) {
                    throw CustomException(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR, errorMsg)
                }
                throw CustomException(javax.ws.rs.core.Response.Status.BAD_REQUEST, errorMsg)
            } else {
                var jsonData = ""
                val operationResult: T? =
                    if (null != jobCloudResp.data) {
                        jsonData = mapper.writeValueAsString(jobCloudResp.data)
                        mapper.readValue(jsonData, classOfT)
                    } else {
                        null
                    }
                if (logger.isDebugEnabled)
                    logger.debug("[$operationName] serialized jsonData: ${logWithLengthLimit(jsonData)}")
                return JobCloudResult(
                    code = jobCloudResp.code,
                    result = jobCloudResp.result,
                    jobRequestId = jobCloudResp.jobRequestId,
                    data = operationResult
                )
            }
        } catch (exception: Exception) {
            logger.warn("Failed to execute the HTTP request. Exception:", exception)
            throw exception
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
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", \"bk_app_secret\": \"${bkAppSecret}\", " +
            "\"bk_username\": \"$AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE\"}"
        val operationName = getJobOperationName()
        val url = jobCloudApiBaseUrl + postPathMap[operationName]
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
}