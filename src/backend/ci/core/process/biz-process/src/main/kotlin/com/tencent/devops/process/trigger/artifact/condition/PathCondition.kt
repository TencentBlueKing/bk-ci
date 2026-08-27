package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.PathRegexFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

class PathCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val paths = context.factParam.paths ?: emptyList()
        val input = context.triggerParam
        val filter = PathRegexFilter(
            pipelineId = context.pipelineId,
            triggerOnPath = paths,
            includedPaths = input.paths,
            excludedPaths = input.pathsIgnore,
            includedFailedReason = I18Variable(
                code = WebhookI18nConstants.PATH_NOT_MATCH,
                params = listOf()
            ).toJsonStr(),
            excludedFailedReason = I18Variable(
                code = WebhookI18nConstants.PATH_IGNORED,
                params = listOf()
            ).toJsonStr(),
            caseSensitive = true
        )
        val response = WebhookFilterResponse()
        val matched = filter.doFilter(response)
        if (!matched) {
            context.response.failedReason = response.failedReason
        }
        return matched
    }
}
