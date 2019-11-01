package com.tencent.devops.common.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.job.api.pojo.BkJobProperties
import com.tencent.devops.common.job.api.pojo.ExecuteTaskRequest
import com.tencent.devops.common.job.api.pojo.FastExecuteScriptRequest
import com.tencent.devops.common.job.api.pojo.FastPushFileRequest
import com.tencent.devops.common.job.api.pojo.JobException
import com.tencent.devops.common.job.api.pojo.OpenStateFastPushFileRequest
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
            logger.error("start jobDevOpsFastExecuteScript failed")
            throw JobException("start jobDevOpsFastExecuteScript failed")
        }
        return taskInstanceId
    }

    fun fastPushFileDevops(pushFileRequest: FastPushFileRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(pushFileRequest)
        val url = "${jobProperties.url}/service/file/$projectId/push/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("start jobDevOpsFastPushfile failed")
            throw JobException("start jobDevOpsFastPushfile failed")
        }
        return taskInstanceId
    }

    fun openStateFastPushFileDevops(pushFileRequest: OpenStateFastPushFileRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(pushFileRequest)
        val url = "${jobProperties.url}/service/file/$projectId/push/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("start openStateFastPushFileDevops failed")
            throw JobException("start openStateFastPushFileDevops failed")
        }
        return taskInstanceId
    }

    fun executeTaskDevops(executeTaskRequest: ExecuteTaskRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(executeTaskRequest)
        val url = "${jobProperties.url}/service/task/$projectId/${executeTaskRequest.taskId}/execute/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("start jobDevOpsFastPushfile failed")
            throw JobException("start jobDevOpsFastPushfile failed")
        }
        return taskInstanceId
    }

    fun getTaskLastModifyUser(projectId: String, taskId: Int): String {
        try {
            val url = "${jobProperties.url}/service/task/$projectId/$taskId/detail"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.get(url)
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["status"] == 0) {
                    val responseData = response["data"] as Map<String, Any>
                    return responseData["lastModifyUser"] as String
                } else {
                    val msg = response["message"] as String
                    logger.error("Get job lastModifyUser failed, msg: $msg")
                    throw JobException("Get job lastModifyUser failed, msg: $msg")
                }
            }
        } catch (e: Exception) {
            logger.error("Get job lastModifyUser error", e)
            throw RuntimeException("Get job lastModifyUser error: ${e.message}")
        }
    }

    fun getTaskResult(projectId: String, taskInstanceId: Long, operator: String): TaskResult {
        try {
            val url = "${jobProperties.url}/service/history/$projectId/$taskInstanceId/status"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.get(url)
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                if (response["status"] == 0) {
                    val responseData = response["data"] as Map<String, Any>
                    val status = responseData["status"] as Int
                    return when (status) {
                        3 -> {
                            logger.info("Job execute task finished and success")
                            TaskResult(true, true, "Success")
                        }
                        4 -> {
                            logger.error("Job execute task failed")
                            TaskResult(true, false, "Job failed")
                        }
                        else -> {
                            logger.info("Job execute task running")
                            TaskResult(false, false, "Job Running")
                        }
                    }
                } else {
                    val msg = response["message"] as String
                    logger.error("job execute failed, msg: $msg")
                    throw JobException("job execute failed, msg: $msg")
                }
            }
        } catch (e: Exception) {
            logger.error("execute job error", e)
            throw RuntimeException("execute job error: ${e.message}")
        }
    }

    private fun sendTaskRequest(requestBody: String, url: String): Long {
        logger.info("request url: $url")
        logger.info("request body: $requestBody")
        val httpReq = Request.Builder()
            .url(url)
            .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            .build()
        OkhttpUtils.doHttp(httpReq).use { resp ->
            val responseStr = resp.body()!!.string()
//        val responseStr = HttpUtils.postJson(url, requestBody)
            logger.info("response body: $responseStr")

            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
            if (response["status"] == 0) {
                val responseData = response["data"] as Map<String, Any>
                val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                logger.info("start job success, taskInstanceId: $taskInstanceId")
                return taskInstanceId
            } else {
                val msg = response["message"] as String
                logger.error("start job failed, msg: $msg")
                throw JobException("start job failed, msg: $msg")
            }
        }
    }

    fun getDetailUrl(projectId: String, taskInstanceId: Long): String {
        return "<a target='_blank' href='${jobProperties.linkUrl}/$projectId/?taskInstanceList&projectId=$projectId#taskInstanceId=$taskInstanceId'>查看详情</a>"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobClient::class.java)
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}
