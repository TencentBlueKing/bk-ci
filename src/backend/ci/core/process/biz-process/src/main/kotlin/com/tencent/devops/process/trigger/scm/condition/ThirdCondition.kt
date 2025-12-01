package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.ThirdFilter
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.MDC

/**
 * 关键字过滤条件
 */
class ThirdCondition(
    private val client: Client,
    private val gitScmService: GitScmService,
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry?
) : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        if (context.webhookParams.enableThirdFilter != true) {
            return true
        }
        val requestId = MDC.get(TraceTag.BIZID)
        // 原始的请求body
        val requestBody = requestId?.let {
            client.get(ServiceRepositoryWebhookResource::class).getWebhookRequest(
                requestId = it
            ).data?.requestBody
        } ?: return true
        with(context.webhookParams) {
            return ThirdFilter(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                event = JsonUtil.to(requestBody, GitEvent::class.java),
                changeFiles = context.factParam.changes.toSet(),
                enableThirdFilter = enableThirdFilter,
                thirdUrl = thirdUrl,
                secretToken = gitScmService.getCredential(context.projectId, thirdSecretToken),
                callbackCircuitBreakerRegistry = callbackCircuitBreakerRegistry,
                failedReason = I18Variable(code = WebhookI18nConstants.THIRD_FILTER_NOT_MATCH).toJsonStr(),
                eventType = context.factParam.eventType
            ).doFilter(context.response)
        }
    }
}
