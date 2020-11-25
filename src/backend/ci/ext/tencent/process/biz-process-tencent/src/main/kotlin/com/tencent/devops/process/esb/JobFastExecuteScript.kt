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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.esb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class JobFastExecuteScript @Autowired constructor(private val buildLogPrinter: BuildLogPrinter) {
    @Value("\${esb.url}")
    private val esbUrl = "http://open.oa.com/component/compapi/job/"

    @Value("\${esb.code}")
    protected val appCode = ""

    @Value("\${esb.secret}")
    protected val appSecret = ""

    fun fastExecuteScript(
        buildId: String,
        operator: String,
        appId: Int,
        content: String,
        scriptParam: String,
        ipList: List<SourceIp>,
        elementId: String,
        containerHashId: String?,
        executeCount: Int,
        type: Int = 1,
        account: String = "root"
    ): Long {
        checkParam(operator, appId, content, account)

        val taskInstanceId = sendTaskRequest(buildId, operator, appId, content, scriptParam, type, ipList, account, elementId, containerHashId, executeCount)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("Job start execute script failed.")
            throw OperationException("Job执行脚本失败")
        }
        return taskInstanceId
    }

    protected fun checkParam(operator: String, appId: Int, content: String, account: String) {
        if (operator.isBlank()) {
            throw ParamBlankException("Invalid operator")
        }
        if (appId <= 0) {
            throw ParamBlankException("Invalid appId")
        }
        if (content.isBlank()) {
            throw ParamBlankException("Invalid content, content is empty")
        }
        try {
            Base64.getDecoder().decode(content)
        } catch (e: IllegalArgumentException) {
            throw ParamBlankException("Invalid content, it's not in valid Base64 scheme")
        }
        if (account.isBlank()) {
            throw ParamBlankException("Invalid account")
        }
    }

    fun checkStatus(
        startTime: Long,
        timeoutSeconds: Int,
        targetAppId: Int,
        taskInstanceId: Long,
        buildId: String,
        taskId: String,
        containerHashId: String?,
        executeCount: Int,
        operator: String
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > timeoutSeconds * 1000) {
            logger.warn("job timeout. timeout seconds:$timeoutSeconds")
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "Job timeout:$timeoutSeconds seconds",
                tag = taskId,
                jobId = containerHashId,
                executeCount = executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = getTaskResult(targetAppId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(buildId, "Job success! jobId:$taskInstanceId", taskId, containerHashId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(buildId, "Job fail! jobId:$taskInstanceId", taskId, containerHashId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            logger.info("[$buildId]|Waiting for job! jobId:$taskInstanceId")
            BuildStatus.LOOP_WAITING
        }
    }

    fun getTaskResult(appId: Int, taskInstanceId: Long, operator: String): TaskResult {
        val url = esbUrl + "get_task_result"
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["task_instance_id"] = taskInstanceId
        requestData["operator"] = operator
        return doGetTaskResult(url, requestData, taskInstanceId)
    }

    protected fun doGetTaskResult(url: String, requestData: Map<String, Any>, taskInstanceId: Long): TaskResult {
        val json = ObjectMapper().writeValueAsString(requestData)
        try {
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, json))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, *>
                    val isFinished = responseData["isFinished"] as Boolean
                    logger.info("request success. taskInstanceId: $taskInstanceId")
                    return if (isFinished) {
                        val taskInstanceObj = responseData["taskInstance"] as Map<String, *>
                        val status = taskInstanceObj["status"] as Int
                        if (status == SUCCESS) {
                            logger.info("Job execute task finished and success")
                            TaskResult(isFinish = true, success = true, msg = "Success")
                        } else {
                            logger.info("Job execute task finished but failed")
                            TaskResult(isFinish = true, success = false, msg = "Job failed")
                        }
                    } else {
                        TaskResult(isFinish = false, success = false, msg = "Job Running")
                    }
                } else {
                    val msg = response["message"] as String
                    logger.error("request failed, msg: $msg")
                    return TaskResult(isFinish = true, success = false, msg = msg)
                }
            }
        } catch (e: Exception) {
            logger.error("error occur", e)
            throw RuntimeException("error occur while execute job task.")
        }
    }

    private fun sendTaskRequest(
        buildId: String,
        operator: String,
        appId: Int,
        content: String,
        scriptParam: String,
        type: Int,
        ipList: List<SourceIp>,
        account: String,
        elementId: String,
        containerHashId: String?,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["content"] = content
        requestData["script_param"] = scriptParam
        requestData["type"] = type
        requestData["account"] = account
        requestData["ip_list"] = ipList
        requestData["operator"] = operator

        val url = esbUrl + "fast_execute_script"
        return doSendTaskRequest(url, requestData, buildId, elementId, containerHashId, executeCount)
    }

    protected fun doSendTaskRequest(url: String, requestData: MutableMap<String, Any>, buildId: String, elementId: String, containerHashId: String?, executeCount: Int): Long {
        val json = ObjectMapper().writeValueAsString(requestData)
        logger.info("send execute script task request: $json")
        try {

            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, json))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                logger.info("send execute script task response: $response")

                return if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, *>
                    val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                    logger.info("request success. taskInstanceId: $taskInstanceId")
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "start execute job task success: taskInstanceId:  $taskInstanceId",
                        tag = elementId,
                        jobId = containerHashId,
                        executeCount = executeCount
                    )
                    taskInstanceId
                } else {
                    val msg = response["message"] as String
                    logger.error("request failed, msg: $msg")
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "start execute job task failed: $msg",
                        tag = elementId,
                        jobId = containerHashId,
                        executeCount = executeCount
                    )
                    -1
                }
            }
        } catch (e: Exception) {
            logger.error("error occur", e)
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "error occur while execute job task: ${e.message}",
                tag = elementId,
                jobId = containerHashId,
                executeCount = executeCount
            )
            throw RuntimeException("error occur while execute job task.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobFastExecuteScript::class.java)
        private const val SUCCESS = 3
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}