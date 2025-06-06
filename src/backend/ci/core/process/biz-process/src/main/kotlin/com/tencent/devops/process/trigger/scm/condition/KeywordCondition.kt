package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.KeywordSkipFilter

/**
 * 关键字过滤条件
 */
class KeywordCondition(private val filterType: KeyWordType) : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        with(context.webhookParams) {
            val (failedReason, keyWord, triggerOnMessage) = when (filterType) {
                KeyWordType.SKIP_WIP -> Triple(
                    I18Variable(WebhookI18nConstants.MR_SKIP_WIP).toJsonStr(),
                    KeywordSkipFilter.KEYWORD_SKIP_WIP,
                    context.factParam.title
                )

                else -> Triple(
                    "",
                    KeywordSkipFilter.KEYWORD_SKIP_CI,
                    context.factParam.lastCommitMsg
                )
            }
            val actionFilter = KeywordSkipFilter(
                pipelineId = context.pipelineId,
                enable = if (filterType == KeyWordType.SKIP_WIP) {
                    skipWip
                } else {
                    true
                },
                keyWord = keyWord,
                triggerOnMessage = triggerOnMessage,
                failedReason = failedReason
            )
            return actionFilter.doFilter(context.response)
        }
    }
}

enum class KeyWordType(val type: String) {
    SKIP_WIP("skip_wip"),
    SKIP_CI("skip_ci")
}
