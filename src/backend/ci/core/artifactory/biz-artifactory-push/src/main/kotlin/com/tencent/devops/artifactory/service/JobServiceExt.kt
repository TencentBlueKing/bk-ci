package com.tencent.devops.artifactory.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.constant.PushMessageCode
import com.tencent.devops.artifactory.pojo.FastPushFileRequest
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JobServiceExt @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${job.nginx.url:#{null}}")
    val jobUrl: String? = null

    fun fastPushFileDevops(pushFileRequest: FastPushFileRequest, projectId: String): Long {
        val requestBody = objectMapper.writeValueAsString(pushFileRequest)
        val url = "$jobUrl/service/file/$projectId/push/"
        val taskInstanceId = sendTaskRequest(requestBody, url)
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.warn("start jobDevOpsFastPushfile failed")
            throw RuntimeException(MessageCodeUtil.getCodeLanMessage(PushMessageCode.JOB_EXECUTE_FAIL))
        }
        return taskInstanceId
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
            logger.info("response body: $responseStr")

            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
            if (response["status"] == 0) {
                val responseData = response["data"] as Map<String, Any>
                val taskInstanceId = responseData["taskInstanceId"].toString().toLong()
                logger.info("start job success, taskInstanceId: $taskInstanceId")
                return taskInstanceId
            } else {
                val msg = response["message"] as String
                logger.warn("start job failed, msg: $msg")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.JOB_EXECUTE_FAIL, arrayOf(msg)))
            }
        }
    }

    fun getTaskResult(projectId: String, taskInstanceId: Long, operator: String): TaskResult {
        try {
            val url = "$jobUrl/service/history/$projectId/$taskInstanceId/status"
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
                    throw RuntimeException("job execute failed, msg: $msg")
                }
            }
        } catch (e: Exception) {
            logger.error("execute job error", e)
            throw RuntimeException("execute job error: ${e.message}")
        }
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}