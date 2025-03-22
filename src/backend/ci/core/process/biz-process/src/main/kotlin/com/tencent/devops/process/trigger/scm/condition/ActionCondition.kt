package com.tencent.devops.process.trigger.scm.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.util.WebhookUtils.convert

/**
 * 动作过滤条件
 */
class ActionCondition(private val filterType: ActionFilterType) : WebhookCondition {
    override fun match(context: WebhookConditionContext): Boolean {
        with(context.webhookParams) {
            val messageCode = when (filterType) {
                ActionFilterType.PUSH -> WebhookI18nConstants.PUSH_ACTION_NOT_MATCH
                ActionFilterType.PULL_REQUEST -> WebhookI18nConstants.MR_ACTION_NOT_MATCH
                ActionFilterType.ISSUE -> WebhookI18nConstants.ISSUES_ACTION_NOT_MATCH
                ActionFilterType.Note -> WebhookI18nConstants.NOTE_ACTION_NOT_MATCH
                ActionFilterType.REVIEW -> WebhookI18nConstants.REVIEW_ACTION_NOT_MATCH
            }
            val includedAction = when (filterType) {
                ActionFilterType.PUSH -> convert(includePushAction).ifEmpty {
                    listOf("empty-action")
                }

                ActionFilterType.PULL_REQUEST -> convert(includeMrAction).ifEmpty {
                    listOf("empty-action")
                }

                ActionFilterType.ISSUE -> convert(includeIssueAction)

                ActionFilterType.Note -> convert(includeNoteTypes)
                ActionFilterType.REVIEW -> convert(includeCrState)
            }
            val triggerOnAction = context.factParam.action
            val actionFilter = ContainsFilter(
                pipelineId = context.pipelineId,
                included = includedAction,
                triggerOn = triggerOnAction,
                filterName = filterType.filterName,
                failedReason = I18Variable(
                    code = messageCode,
                    params = listOf(triggerOnAction)
                ).toJsonStr()
            )
            return actionFilter.doFilter(context.response)
        }
    }
}

enum class ActionFilterType(val filterName: String) {
    PUSH("pushActionFilter"),
    PULL_REQUEST("mrAction"),
    ISSUE("issueAction"),
    Note("noteAction"),
    REVIEW("reviewAction")
}
