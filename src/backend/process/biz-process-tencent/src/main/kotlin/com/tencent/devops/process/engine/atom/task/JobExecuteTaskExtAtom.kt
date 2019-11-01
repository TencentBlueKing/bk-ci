@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.jsonMediaType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.JobExecuteTaskExtElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.project.api.service.ServiceProjectResource
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class JobExecuteTaskExtAtom @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
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
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = buildId,
            containerId = task.containerHashId,
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
                LogUtils.addLine(rabbitTemplate, task.buildId, "找不到绑定配置平台的业务ID/can not found CC Business ID", task.taskId,
                    containerId, executeCount
                )
                return defaultFailAtomResponse
            }
        if (appId < 0) {
            LogUtils.addRedLine(rabbitTemplate, task.buildId, "绑定配置平台的业务ID错误/appId is not init", task.taskId, containerId, executeCount)
            return defaultFailAtomResponse
        }

        if (param.taskId < 0) {
            LogUtils.addRedLine(rabbitTemplate, task.buildId, "绑定的作业模板ID错误/taskId is not init", task.taskId, containerId, executeCount)
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
            LogUtils.addLine(rabbitTemplate, task.buildId, "作业执行失败/start job failed", task.taskId, containerId, executeCount)
            return defaultFailAtomResponse
        }
        val startTime = System.currentTimeMillis()

        val buildStatus = checkStatus(
            startTime = startTime,
            maxRunningMills = timeout,
            appId = appId,
            taskId = task.taskId,
            taskInstanceId = taskInstanceId,
            operator = operator,
            buildId = task.buildId,
            containerId = containerId,
            executeCount = executeCount
        )

        task.taskParams[APP_ID] = appId
        task.taskParams[STARTER] = operator
        task.taskParams[JOB_TASK_ID] = taskInstanceId
        task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime

        if (!BuildStatus.isFinish(buildStatus)) {
            LogUtils.addLine(rabbitTemplate, task.buildId, "作业执行中/Waiting for job:$taskInstanceId", task.taskId, containerId, executeCount)
        }
        return if (buildStatus == BuildStatus.FAILED) AtomResponse(
            buildStatus = BuildStatus.FAILED,
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
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
            throw RuntimeException("execute job error: ${e.message}")
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
        containerId: String?,
        executeCount: Int
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMills) {
            logger.warn("job getTimeout. getTimeout minutes:${maxRunningMills / 60000}")
            LogUtils.addRedLine(
                rabbitTemplate = rabbitTemplate,
                buildId = buildId,
                message = "执行超时/Job getTimeout: ${maxRunningMills / 60000} Minutes",
                tag = taskId,
                jobId = containerId,
                executeCount = executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = getTaskResult(appId, taskInstanceId, operator)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addRedLine(rabbitTemplate, buildId, taskResult.msg, taskId, containerId, executeCount)
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
                    LogUtils.addLine(
                        rabbitTemplate = rabbitTemplate,
                        buildId = task.buildId,
                        message = "执行成功/start job success, taskInstanceId: $taskInstanceId",
                        tag = task.taskId,
                        jobId = task.containerHashId,
                        executeCount = executeCount
                    )
                    LogUtils.addLine(
                        rabbitTemplate = rabbitTemplate,
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
                    LogUtils.addRedLine(
                        rabbitTemplate = rabbitTemplate,
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
            LogUtils.addRedLine(
                rabbitTemplate = rabbitTemplate,
                buildId = task.buildId,
                message = "执行发生异常/start job exception: ${e.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = executeCount
            )
            throw RuntimeException("start job exception")
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
