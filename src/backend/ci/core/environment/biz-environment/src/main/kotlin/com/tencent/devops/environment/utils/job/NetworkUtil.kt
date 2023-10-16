package com.tencent.devops.environment.utils.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.service.job.AuthenticationService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

object NetworkUtil {
    private const val LOG_OUTPUT_MAX_LENGTH = 4000
    private val logger = LoggerFactory.getLogger(NetworkUtil::class.java)

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
        OkhttpUtils.doHttp(request).use { response ->
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