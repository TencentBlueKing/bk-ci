package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 制品名称条件（仅流水线仓库）：复用 [BranchFilter] 对制品名做 include/exclude 通配匹配。
 *
 * 文件用文件名、目录用流水线仓库相对路径，作为 triggerOnBranchName；
 * artifactsName / artifactsNameIgnore 作为 include / exclude 表达式。
 */
class ArtifactsNameCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val input = context.triggerParam
        val artifactsName = context.factParam.artifactsName ?: ""
        val filter = BranchFilter(
            pipelineId = context.pipelineId,
            triggerOnBranchName = artifactsName,
            includedBranches = input.artifactsName,
            excludedBranches = input.artifactsNameIgnore,
            includedFailedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_NAME_NOT_MATCH,
                params = listOf()
            ).toJsonStr(),
            excludedFailedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_NAME_IGNORED,
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
