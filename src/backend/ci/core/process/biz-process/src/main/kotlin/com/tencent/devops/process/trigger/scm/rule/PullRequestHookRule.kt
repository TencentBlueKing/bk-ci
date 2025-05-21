package com.tencent.devops.process.trigger.scm.rule

import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.trigger.scm.condition.ActionCondition
import com.tencent.devops.process.trigger.scm.condition.ActionFilterType
import com.tencent.devops.process.trigger.scm.condition.BranchCondition
import com.tencent.devops.process.trigger.scm.condition.BranchFilterType
import com.tencent.devops.process.trigger.scm.condition.KeyWordType
import com.tencent.devops.process.trigger.scm.condition.KeywordCondition
import com.tencent.devops.process.trigger.scm.condition.PathCondition
import com.tencent.devops.process.trigger.scm.condition.ThirdCondition
import com.tencent.devops.process.trigger.scm.condition.UserCondition
import com.tencent.devops.process.trigger.scm.condition.WebhookConditionChain
import com.tencent.devops.process.trigger.scm.condition.WebhookConditionContext
import com.tencent.devops.process.trigger.scm.condition.WebhookFactParam
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestHook
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PullRequestHookRule @Autowired constructor(
    private val gitScmService: GitScmService,
    // stream没有这个配置
    @Autowired(required = false)
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry? = null
) : WebhookRule {
    override fun support(webhook: Webhook): Boolean {
        return webhook is PullRequestHook
    }

    override fun evaluate(
        projectId: String,
        pipelineId: String,
        webHookParams: WebHookParams,
        webhook: Webhook
    ): WebhookMatchResult {
        val factParam = with(webhook as PullRequestHook) {
            WebhookFactParam(
                userId = userName,
                eventType = eventType,
                action = action.value,
                branch = pullRequest.targetRef.name,
                sourceBranch = pullRequest.sourceRef.name,
                changes = WebhookRuleUtils.getChangeFiles(changes ?: listOf()),
                title = pullRequest.title,
                lastCommitMsg = commit.message ?: ""
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
            KeywordCondition(KeyWordType.SKIP_WIP),
            KeywordCondition(KeyWordType.SKIP_CI),
            ActionCondition(ActionFilterType.PULL_REQUEST),
            BranchCondition(BranchFilterType.TARGET_BRANCH),
            BranchCondition(BranchFilterType.SOURCE_BRANCH),
            PathCondition(),
            UserCondition(),
            ThirdCondition(webhook, gitScmService, callbackCircuitBreakerRegistry)
        )
        return WebhookConditionChain(conditions).match(context)
    }
}
