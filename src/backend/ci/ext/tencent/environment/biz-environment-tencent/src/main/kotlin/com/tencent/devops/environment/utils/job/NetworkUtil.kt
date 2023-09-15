package com.tencent.devops.environment.utils.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.service.IamEsbService.Companion.objectMapper
import com.tencent.devops.environment.permission.impl.TxV3EnvironmentPermissionService.Companion.logger
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody

object NetworkUtil {
    fun <T> createPostRequest(url: String, bkAuthorization: String, jobCloudReq: Class<T>): Request {
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

    fun executeHttpRequest(operateName: String, request: Request): JobCloudResp {
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
                logger.info("[${operateName}] responseBody: $responseBody")

                val serializedRespBody = jacksonObjectMapper().readValue<JobCloudResp>(responseBody!!)

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
                exception.printStackTrace()
                throw exception
            }
        }
    }
}