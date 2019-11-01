package com.tencent.devops.process.esb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.util.CommonUtils
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JobFastPushFile @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
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
        executeCount: Int
    ): Long {
        checkParam(operator, appId, sourceFileList, targetPath)

        val taskInstanceId =
            sendTaskRequest(buildId, operator, appId, sourceFileList, targetIpList, targetPath, elementId, executeCount)
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
        executeCount: Int,
        userId: String
    ): BuildStatus {

        if (System.currentTimeMillis() - startTime > maxRunningMins * 60 * 1000) {
            logger.warn("job timeout. timeout minutes:$maxRunningMins")
            LogUtils.addRedLine(
                rabbitTemplate,
                buildId,
                "Job timeout:$maxRunningMins Minutes",
                taskId,
                executeCount
            )
            return BuildStatus.EXEC_TIMEOUT
        }

        val taskResult = getTaskResult(targetAppId, taskInstanceId, userId)

        return if (taskResult.isFinish) {
            if (taskResult.success) {
                logger.info("[$buildId]|SUCCEED|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addLine(rabbitTemplate, buildId, "Job success! jobId:$taskInstanceId", taskId, executeCount)
                BuildStatus.SUCCEED
            } else {
                logger.info("[$buildId]|FAIL|taskInstanceId=$taskId|${taskResult.msg}")
                LogUtils.addLine(rabbitTemplate, buildId, "Job fail! jobId:$taskInstanceId", taskId, executeCount)
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
        return doSendTaskRequest(url, requestData, buildId, elementId, executeCount)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun doSendTaskRequest(
        url: String,
        requestData: MutableMap<String, Any>,
        buildId: String,
        elementId: String,
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
                    LogUtils.addLine(
                        rabbitTemplate,
                        buildId,
                        "start execute job task success: taskInstanceId:  $taskInstanceId",
                        elementId,
                        executeCount
                    )
                    taskInstanceId
                } else {
                    val msg = response["message"] as String
                    logger.error("request failed, msg: $msg")
                    LogUtils.addLine(
                        rabbitTemplate,
                        buildId,
                        "start execute job task failed: $msg",
                        elementId,
                        executeCount
                    )
                    -1
                }
            }
        } catch (e: Exception) {
            logger.error("error occur", e)
            LogUtils.addLine(
                rabbitTemplate,
                buildId,
                "error occur while execute job task: ${e.message}",
                elementId,
                executeCount
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