package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.RegexContainFilter
import com.tencent.devops.common.webhook.util.WebhookUtils.convert

/**
 * 动作过滤条件
 */
class RegexCondition : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        with(context.webhookParams) {
            val actionFilter = RegexContainFilter(
                pipelineId = context.pipelineId,
                included = convert(includeNoteComment),
                triggerOn = context.factParam.comment,
                filterName = "noteCommentAction",
                failedReason = I18Variable(
                    code = WebhookI18nConstants.NOTE_CONTENT_NOT_MATCH,
                    params = listOf()
                ).toJsonStr()
            )
            return actionFilter.doFilter(context.response)
        }
    }
}
