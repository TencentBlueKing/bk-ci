package com.tencent.devops.common.webhook.service.code.filter

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.service.code.pojo.ThirdFilterBody
import com.tencent.devops.common.webhook.service.code.pojo.ThirdFilterResult
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import jakarta.ws.rs.core.Response

class ThirdFilter(
    private val projectId: String,
    private val pipelineId: String,
    private val event: CodeWebhookEvent,
    private val changeFiles: Set<String>?,
    private val enableThirdFilter: Boolean?,
    private val thirdUrl: String?,
    private val secretToken: Pair<Boolean, String>?,
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry?,
    private val failedReason: String = "",
    private val eventType: String
) : WebhookFilter {

    companion object {
        private const val FILTER_TOKEN_HEADER = "X-DEVOPS-FILTER-TOKEN"
        private const val FILTER_EVENT_TYPE_HEADER = "X-DEVOPS-EVENT-TYPE"
        private const val MAX_RETRY_COUNT = 3
        private val logger = LoggerFactory.getLogger(ThirdFilter::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        if (enableThirdFilter == false || thirdUrl.isNullOrBlank() || !OkhttpUtils.validUrl(thirdUrl)) {
            return true
        }
        logger.info("$pipelineId|thirdUrl:$thirdUrl|third filter")
        val filterResult = secretToken != null && try {
            callbackCircuitBreakerRegistry?.let {
                // 熔断处理
                val breaker = callbackCircuitBreakerRegistry.circuitBreaker(thirdUrl)
                breaker.executeCallable {
                    send()
                }
            } ?: send()
        } catch (ignore: Exception) {
            logger.warn("$pipelineId|Failed to call third filter", ignore)
            false
        }
        if (!filterResult && failedReason.isNotBlank()) {
            response.failedReason = failedReason
        }
        return filterResult
    }

    private fun send(): Boolean {
        val body = JsonUtil.toJson(
            ThirdFilterBody(
                projectId = projectId,
                pipelineId = pipelineId,
                event = event,
                changeFiles = changeFiles,
                eventType = eventType
            )
        )
        val builder = Request.Builder()
            .url(thirdUrl!!)
            .post(
                RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), body)
            )
        if (secretToken?.first == true) {
            builder.addHeader(FILTER_TOKEN_HEADER, secretToken.second)
            builder.addHeader(FILTER_EVENT_TYPE_HEADER, eventType)
        }
        return HttpRetryUtils.retry(MAX_RETRY_COUNT) {
            OkhttpUtils.doShortHttp(request = builder.build()).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    throw CustomException(
                        status = Response.Status.fromStatusCode(response.code) ?: Response.Status.BAD_REQUEST,
                        message = "Failed to call third filter|code:${response.code}|data:$data"
                    )
                }
                logger.info("$pipelineId|third filter result:$data")
                JsonUtil.to(data, ThirdFilterResult::class.java).match
            }
        }
    }
}
