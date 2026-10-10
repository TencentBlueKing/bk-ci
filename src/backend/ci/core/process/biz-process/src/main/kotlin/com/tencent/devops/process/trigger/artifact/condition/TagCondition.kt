package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * tag 条件（仅 IMAGE）：复用 [BranchFilter] 对包版本做 include/exclude 通配匹配。
 *
 * 将版本号当作 triggerOnBranchName，tags/tagsIgnore 当作 include/exclude 分支表达式。
 * 使用临时 [WebhookFilterResponse] 承接，避免 filter 写入的 MATCH_BRANCH 污染输出参数，
 * 仅在失败时回传失败原因。
 */
class TagCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val version = context.factParam.version ?: ""
        val params = context.triggerParam
        val filter = BranchFilter(
            pipelineId = context.pipelineId,
            triggerOnBranchName = version,
            includedBranches = params.tags,
            excludedBranches = params.tagsIgnore,
            includedFailedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_TAG_NOT_MATCH,
                params = listOf()
            ).toJsonStr(),
            excludedFailedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_TAG_IGNORED,
                params = listOf()
            ).toJsonStr()
        )
        val response = WebhookFilterResponse()
        val matched = filter.doFilter(response)
        if (!matched) {
            context.response.failedReason = response.failedReason
        }
        return matched
    }
}
