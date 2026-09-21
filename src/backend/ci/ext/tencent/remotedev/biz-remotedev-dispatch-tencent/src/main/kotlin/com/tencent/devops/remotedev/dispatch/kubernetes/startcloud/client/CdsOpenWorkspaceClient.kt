package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.pojo.CdsActiveWorkspace
import java.net.SocketTimeoutException
import java.util.UUID
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CdsOpenWorkspaceClient(
    @Value("\${startCloud.apiUrlAI:}")
    private val apiUrlAI: String,
    @Value("\${startCloud.saasAppId:}")
    private val saasAppId: String,
    @Value("\${startCloud.saasSecret:}")
    private val saasSecret: String
) {
    fun getActiveWorkspace(workspaceName: String): CdsActiveWorkspace? {
        if (workspaceName.isBlank()) {
            throw WorkspaceDispatchException("workspaceName is blank")
        }
        if (apiUrlAI.isBlank() || saasAppId.isBlank()) {
            throw WorkspaceDispatchException(
                "查询活跃数字分身配置为空, apiUrlAI/saasAppId blank"
            )
        }
        val url = buildRequestUrl(workspaceName)
        val id = UUID.randomUUID()
        logger.info("$id|query active workspace url: $url")
        val request = Request.Builder()
            .url(url)
            .header(APP_ID_HEADER, saasAppId)
            .header(SECRET_HEADER, saasSecret)
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body?.string().orEmpty()
                logger.info(
                    "$id|query active workspace response: ${response.code} || $responseContent"
                )
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "查询活跃数字分身接口异常: ${response.code}"
                    )
                }
                return parseResponse(responseContent)
            }
        } catch (e: WorkspaceDispatchException) {
            throw e
        } catch (e: SocketTimeoutException) {
            logger.error("$id|query active workspace SocketTimeoutException", e)
            throw WorkspaceDispatchException(
                errorMessage = "查询活跃数字分身接口超时, url: $url"
            )
        } catch (e: Exception) {
            logger.error("$id|query active workspace exception", e)
            throw WorkspaceDispatchException(
                errorMessage = "查询活跃数字分身接口异常: ${e.message}"
            )
        }
    }

    internal fun parseResponse(responseContent: String): CdsActiveWorkspace? {
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

    private fun buildRequestUrl(workspaceName: String): String {
        val builder = apiUrlAI.removeSuffix("/").toHttpUrl().newBuilder()
        ACTIVE_PATH.trimStart('/').split("/").filter { it.isNotBlank() }.forEach { segment ->
            builder.addPathSegment(segment)
        }
        builder.addQueryParameter("workspaceName", workspaceName)
        return builder.build().toString()
    }

    companion object {
        private const val APP_ID_HEADER = "x-app-id"
        private const val SECRET_HEADER = "x-secret"
        private const val ACTIVE_PATH = "/ai/api/open/workspace/active"
        private val logger = LoggerFactory.getLogger(CdsOpenWorkspaceClient::class.java)
    }
}
