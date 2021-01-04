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
import com.tencent.devops.process.util.CommonUtils
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JobFastPushFile @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter
) {

    @Value("\${esb.url}")
    private val esbUrl = "http://open.oa.com/component/compapi/job/"

    @Value("\${esb.code}")
    protected val appCode = ""

    @Value("\${esb.secret}")
    protected val appSecret = ""

    fun fastPushFile(
        buildId: String,
        operator: String,
        appId: Int,
        sourceFileList: List<String>,
        targetIpList: List<SourceIp>,
        targetPath: String,
//        timeout: Int,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): Long {
        checkParam(operator, appId, sourceFileList, targetPath)

        val taskInstanceId =
            sendTaskRequest(buildId, operator, appId, sourceFileList, targetIpList,
                targetPath, elementId, containerId, executeCount)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("Job start push file failed.")
            throw OperationException("Job推文件失败")
        }
        return taskInstanceId
    }

    protected fun checkParam(operator: String, appId: Int, sourceFileList: List<String>, targetPath: String) {
        if (operator.isBlank()) {
            throw ParamBlankException("Invalid operator")
        }
        if (appId <= 0) {
            throw ParamBlankException("Invalid appId")
        }
        if (sourceFileList.isEmpty()) {
            throw ParamBlankException("Invalid sourceFileList")
        }
        if (targetPath.isBlank()) {
            throw ParamBlankException("Invalid targetPath")
        }
    }

    fun checkStatus(
        startTime: Long,
        maxRunningMins: Int,
        targetAppId: Int,
        taskInstanceId: Long,
        buildId: String,
        taskId: String,
        containerId: String?,
        executeCount: Int,
        userId: String
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMins * 60 * 1000) {
            logger.warn("job timeout. timeout minutes:$maxRunningMins")
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "Job timeout:$maxRunningMins Minutes",
                tag = taskId,
                jobId = containerId,
                executeCount = executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = getTaskResult(targetAppId, taskInstanceId, userId)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "Job success! jobId:$taskInstanceId",
                    tag = taskId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "Job fail! jobId:$taskInstanceId",
                    tag = taskId,
                    jobId = containerId,
                    executeCount = executeCount
                )
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

    @Suppress("UNCHECKED_CAST")
    protected fun doGetTaskResult(url: String, requestData: Map<String, Any>, taskInstanceId: Long): TaskResult {
        val requestStr = ObjectMapper().writeValueAsString(requestData)
        try {
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
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
        sourceFileList: List<String>,
        targetIpList: List<SourceIp>,
        targetPath: String,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId

        val fileSource = mutableListOf<Any>()
        val fileSourceItem = mutableMapOf<String, Any>()
        fileSourceItem["files"] = sourceFileList
        fileSourceItem["account"] = "root"
        val srcIpList = mutableListOf<Any>()
        val srcIpMap = mutableMapOf<String, String>()
        srcIpMap["ip"] = CommonUtils.getInnerIP()
        srcIpMap["source"] = "1"
        srcIpList.add(srcIpMap)
        fileSourceItem["ip_list"] = srcIpList
        fileSource.add(fileSourceItem)
        requestData["file_source"] = fileSource
        requestData["account"] = "root"
        requestData["file_target_path"] = targetPath
        requestData["ip_list"] = targetIpList
        requestData["operator"] = operator
        val url = esbUrl + "fast_push_file"
        return doSendTaskRequest(url, requestData, buildId, elementId, containerId, executeCount)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun doSendTaskRequest(
        url: String,
        requestData: MutableMap<String, Any>,
        buildId: String,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): Long {
        val requestStr = ObjectMapper().writeValueAsString(requestData)
        logger.info("job fast push file request: $requestStr")
        try {
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
                logger.info("job fast push file response: $requestStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, *>
                    val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                    logger.info("request success. taskInstanceId: $taskInstanceId")
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "start execute job task success: taskInstanceId:  $taskInstanceId",
                        tag = elementId,
                        jobId = containerId,
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
                        jobId = containerId,
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
                jobId = containerId,
                executeCount = executeCount
            )
            throw RuntimeException("error occur while execute job task.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobFastPushFile::class.java)
        private const val SUCCESS = 3
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}