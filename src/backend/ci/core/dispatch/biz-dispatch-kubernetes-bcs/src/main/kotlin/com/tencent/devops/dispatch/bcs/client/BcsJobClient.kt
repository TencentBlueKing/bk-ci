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

package com.tencent.devops.dispatch.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.bcs.pojo.BcsJob
import com.tencent.devops.dispatch.bcs.pojo.BcsJobStatus
import com.tencent.devops.dispatch.bcs.pojo.BcsResult
import com.tencent.devops.dispatch.bcs.pojo.resp.BcsTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BcsJobClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: BcsClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsJobClient::class.java)
    }

    fun createJob(
        userId: String,
        job: BcsJob
    ): BcsResult<BcsTaskResp> {
        val url = "/api/v1/devops/job/${job.name}"
        val body = JsonUtil.toJson(job)
        logger.info("createJob request url: $url, body: $body")
        val request = clientCommon.baseRequest(userId, url).post(
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                body
            )
        ).build()
        val responseBody = OkhttpUtils.doHttp(request).body!!.string()
        logger.info("createJob response: ${JsonUtil.toJson(responseBody)}")
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getJobStatus(userId: String, jobName: String): BcsResult<BcsJobStatus> {
        val url = "/api/v1/devops/job/$jobName"
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("getJobStatus request url: $url, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode.toString(),
                    defaultMessage = "Fail to getJobStatus, http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): BcsResult<List<String>> {
        val url = "/api/v1/devops/job/$jobName/logs".also {
            if (sinceTime != null) {
                it.plus("?since_time=$sinceTime")
            }
        }
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("getJobLogs request url: $url, jobName: $jobName, sinceTime: $sinceTime, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorType = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_SYSTEM_ERROR.errorCode.toString(),
                    defaultMessage = "Fail to getJobLogs, http response code: ${response.code}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
