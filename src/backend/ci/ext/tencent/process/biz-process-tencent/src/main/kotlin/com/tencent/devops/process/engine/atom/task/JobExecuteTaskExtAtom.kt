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

@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorCode.USER_TASK_OPERATE_FAIL
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.jsonMediaType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.JobExecuteTaskExtElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.project.api.service.ServiceProjectResource
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class JobExecuteTaskExtAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val client: Client
) : IAtomTask<JobExecuteTaskExtElement> {
    override fun getParamElement(task: PipelineBuildTask): JobExecuteTaskExtElement {
        return JsonUtil.mapTo(task.taskParams, JobExecuteTaskExtElement::class.java)
    }

    @Value("\${esb.url}")
    private val esbUrl = "http://open.oa.com/component/compapi/job/"

    override fun tryFinish(
        task: PipelineBuildTask,
        param: JobExecuteTaskExtElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskInstanceId = task.taskParams[JOB_TASK_ID]?.toString()?.toLong()
            ?: return if (force) defaultFailAtomResponse else AtomResponse(task.status)

        val buildId = task.buildId
        val taskId = task.taskId
        val executeCount = task.executeCount ?: 1

        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val operator = task.taskParams[STARTER] as String

        logger.info("[$buildId]|LOOP|$taskId|JOB_TASK_ID=$taskInstanceId|startTime=$startTime")
        val timeout = getTimeoutMills(param)

        val appId = task.taskParams[APP_ID]?.toString()?.toInt() ?: param.appId
        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            appId = appId,
            taskId = taskId,
                containerHashId = task.containerHashId,
                taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            executeCount = executeCount
        )

        return AtomResponse(buildStatus)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: JobExecuteTaskExtElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val executeCount = task.executeCount ?: 1
        val appId = client.get(ServiceProjectResource::class).get(task.projectId).data?.ccAppId?.toInt()
            ?: run {
                buildLogPrinter.addLine(task.buildId, "找不到绑定配置平台的业务ID/can not found CC Business ID", task.taskId,
                task.containerHashId,
                executeCount
                )
                return defaultFailAtomResponse
            }
        if (appId < 0) {
            buildLogPrinter.addRedLine(task.buildId, "绑定配置平台的业务ID错误/appId is not init", task.taskId, task.containerHashId, executeCount)
            return defaultFailAtomResponse
        }

        if (param.taskId < 0) {
            buildLogPrinter.addRedLine(task.buildId, "绑定的作业模板ID错误/taskId is not init", task.taskId, task.containerHashId, executeCount)
            return defaultFailAtomResponse
        }

        val globalVar = if (!param.globalVar.isNullOrBlank()) {
            parseVariable(param.globalVar, runVariables)
        } else {
            "[]"
        }

        val timeout = getTimeoutMills(param)
        val operator = task.starter

        val taskInstanceId = sendTaskRequest(task, appId, param.taskId, globalVar, operator, executeCount)
        if (taskInstanceId <= 0) {
            // 失败处理
            buildLogPrinter.addLine(task.buildId, "作业执行失败/start job failed", task.taskId, task.containerHashId, executeCount)
            return defaultFailAtomResponse
        }
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            appId = appId,
            taskId = task.taskId,
                containerHashId = task.containerHashId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = task.buildId,
            executeCount = executeCount
        )

        task.taskParams[APP_ID] = appId
        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            buildLogPrinter.addLine(task.buildId, "作业执行中/Waiting for job:$taskInstanceId", task.taskId, task.containerHashId, executeCount)
        }
        return if (buildStatus == BuildStatus.FAILED) AtomResponse(
            buildStatus = BuildStatus.FAILED,
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
            errorMsg = "failed to excute job"
        ) else AtomResponse(buildStatus)
    }

    private fun getTimeoutMills(param: JobExecuteTaskExtElement): Long {
        var timeout = param.timeout * 1000L
        if (timeout <= 0) {
            timeout = 600 * 1000L // 10 min
        }
        return timeout
    }

    private fun getTaskResult(appId: Int, taskInstanceId: Long, operator: String): TaskResult {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["task_instance_id"] = taskInstanceId
        requestData["operator"] = operator

        val json = ObjectMapper().writeValueAsString(requestData)
        try {
            val httpReq = Request.Builder()
                .url(esbUrl + "get_task_result")
                .post(RequestBody.create(jsonMediaType, json))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(url, requestStr)

                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, Any>
                    val isFinished = responseData["isFinished"] as Boolean
                    logger.error("request success. taskInstanceId: $taskInstanceId")
                    return if (isFinished) {
                        val taskInstanceObj = responseData["taskInstance"] as Map<String, Any>
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
            logger.error("execute job error", e)
            throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "execute job error: ${e.message}")
        }
    }

    private fun checkStatus(
        startTime: Long,
        maxRunningMills: Long,
        appId: Int,
        taskInstanceId: Long,
        operator: String,
        buildId: String,
        taskId: String,
        containerHashId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "执行超时/Job getTimeout: ${maxRunningMills / 60000} Minutes",
                tag = taskId,
                    jobId = containerHashId,
                    executeCount = executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = getTaskResult(appId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addLine(buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                buildLogPrinter.addRedLine(buildId, taskResult.msg, taskId, containerHashId, executeCount)
                BuildStatus.FAILED
            }
        } else {
            BuildStatus.LOOP_WAITING
        }
    }

    private fun sendTaskRequest(
        task: PipelineBuildTask,
        appId: Int,
        taskId: Int,
        globalVar: String,
        operator: String,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["task_id"] = taskId
        requestData["global_var"] = jacksonObjectMapper().readValue<List<Any>>(globalVar)
        requestData["operator"] = operator

        val json = ObjectMapper().writeValueAsString(requestData)

        try {
            val httpReq = Request.Builder()
                .url(esbUrl + "execute_task_ext")
                .post(RequestBody.create(jsonMediaType, json))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseContent = resp.body()!!.string()
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
                if (response["code"] == "00") {
                    val responseData = response["data"] as Map<String, Any>
                    val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                    logger.info("start job success, taskInstanceId: $taskInstanceId")
                    buildLogPrinter.addLine(
                        buildId = task.buildId,
                        message = "执行成功/start job success, taskInstanceId: $taskInstanceId",
                        tag = task.taskId,
                            jobId = task.containerHashId,
                executeCount = executeCount
                    )
                    buildLogPrinter.addLine(
                        buildId = task.buildId,
                        message = "Job detail: <a target='_blank' href='http://job.ied.com/?taskInstanceList&appId=$appId#taskInstanceId=$taskInstanceId'>查看详情</a>",
                        tag = task.taskId,
                            jobId = task.containerHashId,
                executeCount = executeCount
                    )

                    return taskInstanceId
                } else {
                    val msg = response["message"] as String
                    logger.error("start job failed, msg: $msg")
                    buildLogPrinter.addRedLine(
                        buildId = task.buildId,
                        message = "执行失败/start job failed, msg: $msg",
                        tag = task.taskId,
                            jobId = task.containerHashId,
                executeCount = executeCount
                    )
                    return -1
                }
            }
        } catch (e: Exception) {
            logger.error("start job exception", e)
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "执行发生异常/start job exception: ${e.message}",
                tag = task.taskId,
                    jobId = task.containerHashId,
                executeCount = executeCount
            )
            throw TaskExecuteException(
                    errorCode = USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "start job exception"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobExecuteTaskExtAtom::class.java)
        private const val appCode = "bkci"
        private const val appSecret = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
        private const val SUCCESS = 3
        private const val JOB_TASK_ID = "_JOB_TASK_ID"
        private const val APP_ID = "_APP_ID"
        private const val STARTER = "_STARTER"
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}
