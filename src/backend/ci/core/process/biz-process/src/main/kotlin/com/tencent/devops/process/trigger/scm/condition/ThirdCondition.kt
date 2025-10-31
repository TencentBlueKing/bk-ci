package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SOURCE_WEBHOOK
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.ThirdFilter
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry

/**
 * 关键字过滤条件
 */
class ThirdCondition(
    private val webhook: Webhook,
    private val gitScmService: GitScmService,
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry?
) : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        if (context.webhookParams.enableThirdFilter != true) {
            return true
        }
        val sourceWebhook = (webhook.outputs()[BK_REPO_SOURCE_WEBHOOK] as? String)?.takeIf {
            it.isNotBlank()
        } ?: return true
        with(context.webhookParams) {
            return ThirdFilter(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                event = JsonUtil.to(sourceWebhook, GitEvent::class.java),
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
