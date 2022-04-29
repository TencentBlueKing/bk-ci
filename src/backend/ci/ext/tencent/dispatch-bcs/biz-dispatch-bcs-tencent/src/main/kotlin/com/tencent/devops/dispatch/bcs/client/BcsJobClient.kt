package com.tencent.devops.dispatch.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.bcs.common.ConstantsMessage
import com.tencent.devops.dispatch.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsJob
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsJobStatus
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsResult
import com.tencent.devops.dispatch.bcs.pojo.bcs.resp.BcsTaskResp
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BcsJobClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: BcsClientCommon
) {

    @Value("\${bcs.apiUrl}")
    val bcsApiUrl: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(BcsJobClient::class.java)
    }

    fun createJob(
        userId: String,
        job: BcsJob
    ): BcsResult<BcsTaskResp> {
        val url = "$bcsApiUrl/api/v1/devops/job/${job.name}"
        val body = JsonUtil.toJson(job)
        logger.info("createJob request url: $url, body: $body")
        val request = clientCommon.baseRequest(userId, url).post(
            RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                body
            )
        ).build()
        val responseBody = OkhttpUtils.doHttp(request).body()!!.string()
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    fun getJobStatus(userId: String, jobName: String): BcsResult<BcsJobStatus> {
        val url = "$bcsApiUrl/api/v1/devops/job/$jobName"
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("getJobStatus request url: $url, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}查询Job status接口异常（Fail to getJobStatus, " +
                        "http response code: ${response.code()}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): BcsResult<List<String>> {
        val url = "$bcsApiUrl/api/v1/devops/job/$jobName/logs".also {
            if (sinceTime != null) {
                it.plus("?since_time=$sinceTime")
            }
        }
        val request = clientCommon.baseRequest(userId, url).get().build()
        logger.info("getJobLogs request url: $url, jobName: $jobName, sinceTime: $sinceTime, staffName: $userId")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("response: $responseContent")
            if (!response.isSuccessful) {
                throw BuildFailureException(
                    ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}获取Job logs接口异常" +
                        "（Fail to getJobLogs, http response code: ${response.code()}"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
