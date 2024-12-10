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
    constructor() {}

    /**
     * 获取AI摘要
     */
    fun getSummary(
        url: String,
        sourceSha: String,
        targetSha: String,
        accessToken: String
    ): CodeGitCopilotSummary {
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
            accessToken = accessToken,
            headers = mapOf()
        )
        val responseContent = response.body!!.string()
        return JsonUtil.to(responseContent, object : TypeReference<CodeGitCopilotSummary>() {})
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
            accessToken = accessToken,
            headers = mapOf()
        )
    }

    private fun doPost(url: String, body: String, accessToken: String, headers: Map<String, String>): Response {
        val watcher = Watcher("access copilot api [$url]")
        watcher.start()
        val response = OkhttpUtils.doPost(
            url = url,
            jsonParam = body,
            headers = mutableMapOf("Authorization" to "Bearer $accessToken").plus(headers)
        )
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
    }
}