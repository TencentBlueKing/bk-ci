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

package com.tencent.devops.common.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.job.api.pojo.BkJobProperties
import com.tencent.devops.common.job.api.pojo.ExecuteTaskRequest
import com.tencent.devops.common.job.api.pojo.FastExecuteScriptRequest
import com.tencent.devops.common.job.api.pojo.FastPushFileRequest
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("UNCHECKED_CAST")
@Component
class JobClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val jobProperties: BkJobProperties
) {

    fun fastExecuteScriptDevops(executeScriptRequest: FastExecuteScriptRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(executeScriptRequest)
        val url = "${jobProperties.url}/service/script/$projectId/execute/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.info("start jobDevOpsFastExecuteScript failed")
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "start jobDevOpsFastExecuteScript failed"
            )
        }
        return taskInstanceId
    }

    fun fastPushFileDevops(pushFileRequest: FastPushFileRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(pushFileRequest)
        val url = "${jobProperties.url}/service/file/$projectId/push/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.info("start jobDevOpsFastPushfile failed")
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "start jobDevOpsFastPushfile failed"
            )
        }
        return taskInstanceId
    }

    fun executeTaskDevops(executeTaskRequest: ExecuteTaskRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(executeTaskRequest)
        val url = "${jobProperties.url}/service/task/$projectId/${executeTaskRequest.taskId}/execute/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.info("start jobDevOpsFastPushfile failed")
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "start jobDevOpsFastPushfile failed"
            )
        }
        return taskInstanceId
    }

    fun getTaskLastModifyUser(projectId: String, taskId: Int): String {
        try {
            val url = "${jobProperties.url}/service/task/$projectId/$taskId/detail"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url, mapOf("X-DEVOPS-JOB-API-TOKEN" to jobProperties.token!!)).use { resp ->
                val responseStr = resp.body!!.string()
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["status"] == 0) {
                    val responseData = response["data"] as Map<String, Any>
                    return responseData["lastModifyUser"] as String
                } else {
                    val msg = response["message"] as String
                    logger.info("Get job lastModifyUser failed, msg: $msg")
                    throw TaskExecuteException(
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                        errorMsg = "Get job lastModifyUser failed, msg: $msg"
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Get job lastModifyUser error", e)
            throw RuntimeException("Get job lastModifyUser error: ${e.message}")
        }
    }

    fun getTaskResult(projectId: String, taskInstanceId: Long, operator: String): TaskResult {
        try {
            val url = "${jobProperties.url}/service/history/$projectId/$taskInstanceId/status"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url, mapOf("X-DEVOPS-JOB-API-TOKEN" to jobProperties.token!!)).use { resp ->
                val responseStr = resp.body!!.string()
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["status"] == 0) {
                    val responseData = response["data"] as Map<String, Any>
                    return when (responseData["status"] as Int) {
                        3 -> {
                            logger.info("Job execute task finished and success")
                            TaskResult(isFinish = true, success = true, msg = "Success")
                        }

                        4 -> {
                            logger.info("Job execute task failed")
                            TaskResult(isFinish = true, success = false, msg = "Job failed")
                        }

                        else -> {
                            logger.info("Job execute task running")
                            TaskResult(isFinish = false, success = false, msg = "Job Running")
                        }
                    }
                } else {
                    val msg = response["message"] as String
                    logger.info("job execute failed, msg: $msg")
                    throw TaskExecuteException(
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                        errorMsg = "job execute failed, msg: $msg"
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("execute job error", e)
            throw RuntimeException("execute job error: ${e.message}")
        }
    }

    private fun sendTaskRequest(requestBody: String, url: String): Long {
        logger.info("request url: $url")
        logger.info("request body: $requestBody")
        val httpReq = Request.Builder()
            .url(url)
            .header("X-DEVOPS-JOB-API-TOKEN", jobProperties.token!!)
            .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            .build()
        OkhttpUtils.doHttp(httpReq).use { resp ->
            val responseStr = resp.body!!.string()
            logger.info("response body: $responseStr")

            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
            if (response["status"] == 0) {
                val responseData = response["data"] as Map<String, Any>
                val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                logger.info("start job success, taskInstanceId: $taskInstanceId")
                return taskInstanceId
            } else {
                val msg = response["message"] as String
                logger.info("start job failed, msg: $msg")
                throw TaskExecuteException(
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "start job failed, msg: $msg"
                )
            }
        }
    }

    fun getDetailUrl(projectId: String, taskInstanceId: Long): String {
        return "<a target='_blank' href='${jobProperties.linkUrl}/$projectId/?taskInstanceList" +
            "&projectId=$projectId#taskInstanceId=$taskInstanceId'>查看详情</a>"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobClient::class.java)
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}
