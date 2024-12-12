package com.tencent.devops.repository.service.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.repository.AISummaryRateType
import com.tencent.devops.repository.pojo.CodeGitCopilotSummary
import com.tencent.devops.repository.service.RepositoryCopilotService
import okhttp3.Response
import org.slf4j.LoggerFactory

/**
 * 工蜂Copilot接口调用
 */
class CopilotApi {
    /**
     * 获取AI摘要
     */
    fun getSummary(
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
        val response = doPost(
            url = "$url?$queryParam",
            body = JsonUtil.toJson(mapOf<String, String>(), false),
            headers = mapOf("Authorization" to "Bearer $accessToken"),
            longRequest = true
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
        doPost(
            url = "$url?$queryParam",
            body = JsonUtil.toJson(mapOf<String, String>(), false),
            headers = mapOf("Authorization" to "Bearer $accessToken")
        )
    }

    private fun doPost(
        url: String,
        body: String,
        headers: Map<String, String>,
        longRequest: Boolean = false
    ): Response {
        val watcher = Watcher("access copilot api [$url]")
        watcher.start()
        val response = if (longRequest) {
            OkhttpUtils.doCustomTimeoutPost(
                connectTimeout = CONNECT_TIMEOUT_SECONDS,
                readTimeout = READ_TIMEOUT_SECONDS,
                writeTimeout = WRITE_TIMEOUT_SECONDS,
                url = url,
                jsonParam = body,
                headers = headers
            )
        } else {
            OkhttpUtils.doPost(
                url = url,
                jsonParam = body,
                headers = headers
            )
        }
        if (!response.isSuccessful) {
            logger.warn("copilot api access failed|$url|${response.code}|${response.body?.string()}")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.THIRD_PARTY_SERVICE_OPERATION_FAILED,
                params = arrayOf("Copilot", response.body?.string() ?: response.code.toString())
            )
        }
        LogUtils.printCostTimeWE(watcher, warnThreshold = 200, errorThreshold = 1000)
        return response
    }

    companion object {
        // 蓝盾侧调用接口固定值
        const val API_REFERER = "landun"
        val logger = LoggerFactory.getLogger(RepositoryCopilotService::class.java)
        // HTTP请求超时时间（秒）
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val WRITE_TIMEOUT_SECONDS = 60L
    }
}