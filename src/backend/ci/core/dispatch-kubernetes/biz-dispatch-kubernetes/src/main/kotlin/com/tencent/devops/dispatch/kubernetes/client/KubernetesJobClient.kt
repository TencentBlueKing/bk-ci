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

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.BK_BUILD_AND_PUSH_INTERFACE_EXCEPTION
import com.tencent.devops.dispatch.kubernetes.pojo.BuildAndPushImage
import com.tencent.devops.dispatch.kubernetes.pojo.Job
import com.tencent.devops.dispatch.kubernetes.pojo.JobStatus
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.TaskResp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KubernetesJobClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesJobClient::class.java)
    }

    fun createJob(
        userId: String,
        job: Job
    ): KubernetesResult<TaskResp> {
        val url = "/api/jobs"
        val body = JsonUtil.toJson(job)
        logger.info("Create job request url: $url, body: $body")
        val request = clientCommon.baseRequest(userId, url).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body
            )
        ).build()
        val responseBody = OkhttpUtils.doHttp(request).body!!.string()
        logger.info("Create job response: ${JsonUtil.toJson(responseBody)}")
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getJobStatus(userId: String, jobName: String): KubernetesResult<JobStatus> {
        val url = "/api/jobs/$jobName/status"
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("Get job: $jobName status request url: $url, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get job: $jobName status response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to getJobStatus,http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): KubernetesResult<String> {
        val url = "/api/jobs/$jobName/log".also {
            if (sinceTime != null) {
                it.plus("?sinceTime=$sinceTime")
            }
        }
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("Get job: $jobName logs request url: $url, jobName: $jobName, " +
                        "sinceTime: $sinceTime, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("Get job: $jobName logs response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_SYSTEM_ERROR.getErrorMessage(),
                    errorMessage = "Fail to getJobLogs, http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun buildAndPushImage(
        userId: String,
        buildImageInfo: BuildAndPushImage
    ): String {
        val url = "/api/jobs/buildAndPushImage"
        logger.info("Build and push image, request url: $url, staffName: $userId")

        val request = clientCommon.baseRequest(userId, url)
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(buildImageInfo)
                )
            )
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId build and push image response: $responseContent")
                if (!response.isSuccessful) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_CREATE_IMAGE_INTERFACE_ERROR.getErrorMessage(),
                        I18nUtil.getCodeLanMessage(BK_BUILD_AND_PUSH_INTERFACE_EXCEPTION) +
                                "（Fail to build image, http response code: ${response.code}"
                    )
                }
                val responseData: KubernetesResult<TaskResp> = objectMapper.readValue(responseContent)

                if (responseData.isOk()) {
                    return responseData.data!!.taskId
                } else {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorType,
                        errorCode = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_CREATE_VM_INTERFACE_FAIL.getErrorMessage(),
                        errorMessage = "Build and mirror interface returns failure: ${responseData.message}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("$userId build and push image failed.", e)
            throw BuildFailureException(
                errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                errorMessage = "Build and push interface timeout, url: $url"
            )
        }
    }
}
