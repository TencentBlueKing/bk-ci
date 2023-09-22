package com.tencent.devops.environment.utils.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.JobCloudResp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

object NetworkUtil {
    private const val LOG_OUTPUT_MAX_LENGTH = 4000
    val logger = LoggerFactory.getLogger(NetworkUtil::class.java)

    fun <T, U> executeHttpRequest(
        httpType: String,
        operateName: String,
        url: String,
        bkAuthorization: String,
        jobCloudReq: Class<T>?
    ): JobCloudResp<U> {
        var request: Request?
        when (httpType) {
            "post" -> {
                request = createPostRequest(url, bkAuthorization, jobCloudReq)
            }

            "get" -> {
                request = createGetRequest(url, bkAuthorization)
            }

            else -> {
                logger.error("[executeHttpRequest] Invalid http type.")
                return JobCloudResp(
                    code = -1,
                    result = false,
                    jobRequestId = "",
                    message = "[executeHttpRequest] Invalid http type.",
                    data = null
                )
            }
        }
        return executeHttpRequest(operateName, request)
    }

    private fun <T> createPostRequest(url: String, bkAuthorization: String, jobCloudReq: Class<T>?): Request {
        return Request.Builder()
            .url(url)
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    ObjectMapper().writeValueAsString(jobCloudReq)
                )
            )
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

    private fun <T> executeHttpRequest(operateName: String, request: Request): JobCloudResp<T> {
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
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
                logger.info("[$operateName] requestBody: $requestLog, responseBody: $responseLog")

                val serializedRespBody = jacksonObjectMapper().readValue<JobCloudResp<T>>(responseBody!!)

                if (!serializedRespBody.result) {
                    logger.error(
                        "[${operateName}] Execute failed! Req ID: ${serializedRespBody.jobRequestId}, " +
                            "Error code: ${serializedRespBody.code}, " +
                            "Error msg: ${serializedRespBody.message}"
                    )
                    throw RemoteServiceException(
                        "Execute failed! Req ID: ${serializedRespBody.jobRequestId}, " +
                            "Error code: ${serializedRespBody.code}, " +
                            "Error msg: ${serializedRespBody.message}"
                    )
                }
                return serializedRespBody
            } catch (exception: Exception) {
                logger.warn("[executeHttpRequest] Failed to execute the HTTP request. Exception:", exception)
                throw exception
            }
        }
    }
}