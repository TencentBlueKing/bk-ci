package com.tencent.devops.process.trigger.scm.rule

import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.trigger.scm.condition.ActionCondition
import com.tencent.devops.process.trigger.scm.condition.ActionFilterType
import com.tencent.devops.process.trigger.scm.condition.WebhookConditionChain
import com.tencent.devops.process.trigger.scm.condition.WebhookConditionContext
import com.tencent.devops.process.trigger.scm.condition.WebhookFactParam
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestReviewHook
import org.springframework.stereotype.Service

@Service
class ReviewHookRule : WebhookRule {
    override fun support(webhook: Webhook): Boolean {
        return webhook is PullRequestReviewHook
    }

    override fun evaluate(
        projectId: String,
        pipelineId: String,
        webHookParams: WebHookParams,
        webhook: Webhook
    ): WebhookMatchResult {
        val factParam = with(webhook as PullRequestReviewHook) {
            WebhookFactParam(
                userId = userName,
                eventType = eventType,
                action = webhook.review.state?.value ?: ""
            )
        }
        val context = WebhookConditionContext(
            projectId = projectId,
            pipelineId = pipelineId,
            webhookParams = webHookParams,
            factParam = factParam,
            response = WebhookFilterResponse()
        )
        val conditions = listOf(
            ActionCondition(ActionFilterType.REVIEW)
        )
        return WebhookConditionChain(conditions).match(context)
    }
}
