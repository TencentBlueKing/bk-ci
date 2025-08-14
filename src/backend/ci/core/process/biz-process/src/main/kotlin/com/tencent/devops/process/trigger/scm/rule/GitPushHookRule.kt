/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import com.tencent.devops.scm.api.pojo.webhook.git.GitPushHook
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * git push事件触发规则
 */
@Service
class GitPushHookRule @Autowired constructor (
    private val gitScmService: GitScmService,
    // stream没有这个配置
    @Autowired(required = false)
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry? = null
) : WebhookRule {
    override fun support(webhook: Webhook): Boolean {
        return webhook is GitPushHook
    }

    override fun evaluate(
        projectId: String,
        pipelineId: String,
        webHookParams: WebHookParams,
        webhook: Webhook
    ): WebhookMatchResult {
        val factParam = with(webhook as GitPushHook) {
            WebhookFactParam(
                userId = userName,
                eventType = eventType,
                action = action.value,
                branch = ref,
                changes = WebhookRuleUtils.getChangeFiles(changes),
                lastCommitMsg = commit?.message ?: ""
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
            KeywordCondition(KeyWordType.SKIP_CI),
            ActionCondition(ActionFilterType.PUSH),
            BranchCondition(BranchFilterType.BRANCH),
            UserCondition(),
            PathCondition(),
            ThirdCondition(webhook, gitScmService, callbackCircuitBreakerRegistry)
        )
        return WebhookConditionChain(conditions).match(context)
    }
}
