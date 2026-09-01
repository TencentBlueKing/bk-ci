package com.tencent.devops.process.engine.service.cds

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.process.pojo.creative.CdsOpenApprovalDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class CdsOpenApprovalClient(
    @Value("\${cds.approval.host:https://cds-sz.bkdevops.qq.com}")
    private val host: String,
    @Value("\${cds.approval.appId:bk-ci-creative-stream}")
    private val appId: String,
    @Value("\${cds.approval.appSecret:}")
    private val appSecret: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CdsOpenApprovalClient::class.java)
        const val ERROR_NOT_FOUND = 2302081
    }

    fun isConfigured(): Boolean = appSecret.isNotBlank() && appId.isNotBlank() && host.isNotBlank()

    fun getDetail(taskId: String): CdsApprovalQuery {
        if (!isConfigured()) {
            return CdsApprovalQuery(configured = false, failed = false, notFound = false, detail = null)
        }
        val encoded = URLEncoder.encode(taskId, StandardCharsets.UTF_8.name())
        val url = "${host.trimEnd('/')}/ai/api/open/approval/detail?taskId=$encoded"
        val headers = mapOf(
            "x-app-id" to appId,
            "x-secret" to appSecret,
            "Accept" to "application/json"
        )
        return try {
            OkhttpUtils.doGet(url, headers).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    logger.warn("CDS approval detail http=${response.code} taskId=$taskId body=$body")
                    return CdsApprovalQuery(configured = true, failed = true, notFound = false, detail = null)
                }
                val parsed = JsonUtil.to(body, object : TypeReference<Result<CdsOpenApprovalDetail>>() {})
                when {
                    parsed.status == 0 -> CdsApprovalQuery(
                        configured = true,
                        failed = false,
                        notFound = false,
                        detail = parsed.data
                    )
                    parsed.status == ERROR_NOT_FOUND -> CdsApprovalQuery(
                        configured = true,
                        failed = false,
                        notFound = true,
                        detail = null
                    )
                    else -> {
                        logger.warn(
                            "CDS approval detail status=${parsed.status} message=${parsed.message} taskId=$taskId"
                        )
                        CdsApprovalQuery(configured = true, failed = true, notFound = false, detail = null)
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("CDS approval detail failed taskId=$taskId", t)
            CdsApprovalQuery(configured = true, failed = true, notFound = false, detail = null)
        }
    }

    data class CdsApprovalQuery(
        val configured: Boolean,
        val failed: Boolean,
        val notFound: Boolean,
        val detail: CdsOpenApprovalDetail?
    )
}
