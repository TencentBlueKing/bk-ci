package com.tencent.devops.scm.services.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.scm.enums.AISummaryRateType
import com.tencent.devops.scm.pojo.CodeGitCopilotSummary
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.net.URL

/**
 * 工蜂Copilot接口调用
 */
class CopilotApi {
    /**
     * 生成AI摘要
     */
    fun createSummary(
        url: String,
        sourceSha: String,
        targetSha: String,
        accessToken: String
    ): CodeGitCopilotSummary? {
        val queryParam = OkhttpUtils.joinParams(
            params = mapOf(
                "source" to sourceSha,
                "target" to targetSha,
                "referer" to API_REFERER
            )
        )
        val response = doHttp(
            request = Request.Builder()
                .url(URL("$url?$queryParam"))
                .header("Authorization", "Bearer $accessToken")
                .post("".toRequestBody(jsonMediaType))
                .build()
        )
        val responseContent = response.body!!.string()
        return if (responseContent.isBlank()) {
            logger.warn("the AI summary result is empty, please check the input parameters")
            null
        } else {
            JsonUtil.to(responseContent, object : TypeReference<CodeGitCopilotSummary>() {})
        }
    }

    /**
     * 获取AI摘要结果
     */
    fun getSummary(
        url: String,
        taskId: String,
        accessToken: String
    ): CodeGitCopilotSummary? {
        val queryParam = OkhttpUtils.joinParams(
            params = mapOf(
                "task_id" to taskId,
                "referer" to API_REFERER
            )
        )
        val response = doHttp(
            request = Request.Builder()
                .url(URL("$url?$queryParam"))
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
        )
        val responseContent = response.body!!.string()
        return if (responseContent.isBlank()) {
            logger.warn("the AI summary result is empty, please check the input parameters")
            null
        } else {
            JsonUtil.to(responseContent, object : TypeReference<CodeGitCopilotSummary>() {})
        }
    }

    fun rateSummary(
        url: String,
        processId: String,
        type: AISummaryRateType,
        feedback: String?,
        accessToken: String
    ) {
        val queryParam = OkhttpUtils.joinParams(
            params = mutableMapOf(
                "process_id" to processId,
                "type" to type.value,
                "referer" to API_REFERER
            ).let {
                if (!feedback.isNullOrBlank()) {
                    it["feedback"] = feedback
                }
                it
            }
        )

        doHttp(
            request = Request.Builder()
                .url(URL("$url?$queryParam"))
                .header("Authorization", "Bearer $accessToken")
                .post("".toRequestBody(jsonMediaType))
                .build()
        )
    }

    private fun doHttp(request: Request): Response {
        val watcher = Watcher("access copilot api [${request.url}]")
        watcher.start()
        val response = OkhttpUtils.doHttp(request)
        if (!response.isSuccessful) {
            logger.warn("copilot api access failed|${request.url}|${response.code}|${response.body?.string()}")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.THIRD_PARTY_SERVICE_OPERATION_FAILED,
                params = arrayOf("Copilot", response.body?.string() ?: response.code.toString())
            )
        }
        watcher.stop()
        return response
    }

    companion object {
        // 蓝盾侧调用接口固定值
        const val API_REFERER = "landun"
        val logger = LoggerFactory.getLogger(CopilotApi::class.java)
        val jsonMediaType = "application/json".toMediaTypeOrNull()
    }
}