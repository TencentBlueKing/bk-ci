package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.pojo.CdsActiveWorkspace
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CdsOpenWorkspaceClient {
    companion object {
        private val logger = LoggerFactory.getLogger(CdsOpenWorkspaceClient::class.java)
    }

    @Value("\${startCloud.apiUrlAI:}")
    val apiUrlAI: String = ""

    @Value("\${startCloud.saasAppId:}")
    val saasAppId: String = ""

    @Value("\${startCloud.saasSecret:}")
    val saasSecret: String = ""

    fun getActiveWorkspace(workspaceName: String): CdsActiveWorkspace? {
        val encodedName = URLEncoder.encode(workspaceName, StandardCharsets.UTF_8.name())
        val url = "$apiUrlAI/ai/api/open/workspace/active?workspaceName=$encodedName"
        val id = UUID.randomUUID()
        logger.info("$id|query active workspace request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info(
                    "$id|query active workspace response: ${response.code} || $responseContent"
                )
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "查询活跃数字分身接口异常: ${response.code}"
                    )
                }
                val result = JsonUtil.to(
                    responseContent,
                    object : TypeReference<Result<CdsActiveWorkspace>>() {}
                )
                if (result.isNotOk()) {
                    throw WorkspaceDispatchException(
                        "查询活跃数字分身接口返回异常: ${result.status}-${result.message}"
                    )
                }
                return result.data
            }
        } catch (e: SocketTimeoutException) {
            logger.error("query active workspace get SocketTimeoutException", e)
            throw WorkspaceDispatchException(
                errorMessage = "查询活跃数字分身接口超时, url: $url"
            )
        }
    }

    fun makeHeaders(): Map<String, String> {
        return mapOf(
            "x-app-id" to saasAppId,
            "x-secret" to saasSecret
        )
    }
}
