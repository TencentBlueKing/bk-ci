@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.GseKitProcRunCmdElementDev
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
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
class GseKitProcRunCmdTaskAtomDev @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<GseKitProcRunCmdElementDev> {

    @Value("\${gsekit.proc}")
    private val gsekitApiUrl = "http://open.oa.com/component/compapi/gse"

    override fun getParamElement(task: PipelineBuildTask): GseKitProcRunCmdElementDev {
        return JsonUtil.mapTo(task.taskParams, GseKitProcRunCmdElementDev::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: GseKitProcRunCmdElementDev, runVariables: Map<String, String>): AtomResponse {

        val procIdValue = parseVariable(param.procId, runVariables)
        val timeout = param.timeout

        var paramsValue = parseVariable(param.params.joinToString(","), runVariables)

        if ("start".equals(param.cmd, true) ||
                "stop".equals(param.cmd, true) ||
                "restart".equals(param.cmd, true) ||
                "kill".equals(param.cmd, true)) {
            paramsValue = "-b=${param.concurrency} $paramsValue"
        }

        // 环境类型，配置平台集群的标准属性；可选值为 1（中文含义：测试环境），2（体验环境），3（正式环境）
        if (1 != param.envId && 2 != param.envId) {
            LogUtils.addRedLine(rabbitTemplate, task.buildId, "envId is not validate", task.taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "envId is not validate"
            )
        }
        val operator = task.starter
        val buildId = task.buildId
        val taskId = task.taskId

        val sessionRs = createSession(param.appId, param.envId, operator)
        if (!sessionRs.success) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "Gsekit create sesssion failed, msg: ${sessionRs.message}", taskId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "Gsekit create sesssion failed, msg: ${sessionRs.message}"
            )
        }
        val sessionId = sessionRs.data as String
        val runProcRs = procRunCmd(param.cmd, procIdValue, paramsValue, operator, sessionId)
        if (!runProcRs.success) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "Gsekit proc run cmd failed, msg: ${runProcRs.message}", taskId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "Gsekit proc run cmd failed, msg: ${runProcRs.message}"
            )
        }

        val startTime = System.currentTimeMillis()
        val gseKitTaskId = runProcRs.data as String
        val buildStatus = checkStatus(startTime, timeout, gseKitTaskId, buildId, taskId, operator, task.executeCount ?: 1)
        if (!BuildStatus.isFinish(buildStatus)) {
            task.taskParams["bsGseKitTaskId"] = gseKitTaskId
            task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime
            task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = 6000
            LogUtils.addLine(rabbitTemplate, buildId, "等待上传结果", taskId, task.executeCount ?: 1)
        }
        return AtomResponse(buildStatus)
    }

    override fun tryFinish(task: PipelineBuildTask, param: GseKitProcRunCmdElementDev, runVariables: Map<String, String>, force: Boolean): AtomResponse {
        val buildId = task.buildId
        if (task.taskParams["bsGseKitTaskId"] == null) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "找不到GseKit任务ID，请联系管理员", task.taskId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = "找不到GseKit任务ID，请联系管理员"
            )
        }
        val gseKitTaskId = task.taskParams["bsGseKitTaskId"].toString()
        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val timeout = param.timeout

        val result = checkStatus(startTime, timeout, gseKitTaskId, buildId, task.taskId, task.starter, task.executeCount ?: 1)

        return if (result == BuildStatus.FAILED) AtomResponse(
            buildStatus = BuildStatus.FAILED,
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
            errorMsg = "Gsekit proc run cmd failed"
        ) else AtomResponse(result)
    }

    private fun checkStatus(startTime: Long, timeout: Int, gseKitTaskId: String, buildId: String, taskId: String, operator: String, executeCount: Int): BuildStatus {
        logger.info("waiting for gsekit done, timeout: $timeout min")
        if (System.currentTimeMillis() - startTime > timeout * 60 * 1000) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "execute gsekit timeout", taskId, executeCount)
            return BuildStatus.FAILED
        }

        val (isFinish, success, msg) = getProcRunCmdResult(gseKitTaskId, operator)
        return when {
            !isFinish -> {
                logger.info("execute gsekit running..")
                BuildStatus.LOOP_WAITING
            }
            !success -> {
                LogUtils.addRedLine(rabbitTemplate, buildId, "execute gsekit failed, msg: $msg", taskId, executeCount)
                BuildStatus.FAILED
            }
            else -> {
                LogUtils.addLine(rabbitTemplate, buildId, "execute gsekit success!", taskId, executeCount)
                BuildStatus.SUCCEED
            }
        }
    }

    private fun getProcRunCmdResult(taskId: String, operator: String): TaskResult {
        val requestData = mutableMapOf<String, Any>()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["operator"] = operator
        requestData["task_id"] = taskId

        val requestStr = ObjectMapper().writeValueAsString(requestData)

        try {
            val url = "$gsekitApiUrl/proc_get_task_result_by_id/"
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(url, requestStr)
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["result"] == false) {
                    val msg = response["msg"].toString()
                    logger.error("Request gse kit proc_run_command failed, response: $msg")
                    return TaskResult(true, false, msg)
                } else {
                    val data = response["data"] as Map<*, *>
                    val errorCode = data["error_code"] as Int
                    return if (0 == errorCode) {
                        val successInfo = ObjectMapper().writeValueAsString(data["success"])
                        TaskResult(true, true, successInfo)
                    } else if (804 == errorCode || 180 == errorCode || 185 == errorCode) { // 和GSEKit确认，这几个返回码表示任务需要继续轮询
                        TaskResult(false, true, "")
                    } else {
                        val failed = data["failed"] as List<Map<String, Any>>
                        var msg = if (failed.isEmpty()) {
                            data["error_msg"] as String
                        } else {
                            failed[0]["content"] as String
                        }
                        if (msg.isBlank()) {
                            msg = response["message"].toString()
                        }
                        logger.error("Request gse kit proc_run_command failed, error msg: $msg")
                        TaskResult(true, false, msg)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Get gse kit proc run cmd result error", e)
            return TaskResult(true, false, "Get gse kit proc run cmd result error")
        }
    }

    private fun procRunCmd(
        cmd: String,
        procId: String,
        param: String,
        operator: String,
        sessionId: String
    ): CallResult<String> {
        val requestData = mutableMapOf<String, Any>()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["operator"] = operator

        requestData["session_id"] = sessionId
        requestData["cmd"] = cmd
        requestData["proc_id"] = procId
        requestData["params"] = param.split(",")
        requestData["ipaddr"] = "127.0.0.1"

        val requestStr = JsonUtil.toJson(requestData)

        try {
            val url = "$gsekitApiUrl/proc_run_command/"
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
                //            val responseStr = HttpUtils.postJson(url, requestStr)
                val response: Map<String, Any> = JsonUtil.getObjectMapper().readValue(responseStr)
                if (response["result"] == false) {
                    logger.error("request failed")
                    val msg = response["msg"].toString()
                    logger.error("Request gsekit proc_run_command failed, response: $msg")
                    return CallResult(false, "", msg)
                } else {
                    val data = response["data"] as Map<*, *>
                    val errorCode = data["error_code"] as Int
                    return if (0 != errorCode) {
                        logger.error("request failed")
                        val msg = data["error_msg"].toString()
                        logger.error("Request gsekit proc_run_command failed, error msg: $msg")
                        CallResult(false, "", msg)
                    } else {
                        val taskId = data["task_id"] as String
                        CallResult(true, taskId, "success")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("start gse kit proc run cmd error", e)
            return CallResult(false, "", "start gse kit proc run cmd error")
        }
    }

    private fun createSession(appId: Int, envId: Int, operator: String): CallResult<String> {
        val requestData = mutableMapOf<String, Any>()

        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["username"] = operator
        requestData["operator"] = operator
        requestData["app_id"] = appId
        requestData["env_id"] = envId

        val requestStr = ObjectMapper().writeValueAsString(requestData)

        try {
            val url = "$gsekitApiUrl/proc_create_session/"
            val httpReq = Request.Builder()
                .url(url)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestStr))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(url, requestStr)
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["result"] == false) {
                    logger.error("request failed")
                    val msg = response["msg"].toString()
                    logger.error("Request gsekit proc_create_session failed, response: $msg")
                    return CallResult(false, "", msg)
                } else {
                    val data = response["data"] as Map<*, *>
                    val errorCode = data["error_code"] as Int
                    return if (0 != errorCode) {
                        logger.error("request failed")
                        val msg = data["error_msg"].toString()
                        logger.error("Request gsekit proc_create_session failed, error msg: $msg")
                        CallResult(false, "", msg)
                    } else {
                        val sessionId = data["session_id"] as String
                        CallResult(true, sessionId, "success")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("start gse kit create session error", e)
            return CallResult(false, "", "start gse kit create session error")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GseKitProcRunCmdTaskAtomDev::class.java)
        private const val appCode = "bkci"
        private const val appSecret = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
    }

    data class CallResult<T>(val success: Boolean, val data: T?, val message: String)
    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}
