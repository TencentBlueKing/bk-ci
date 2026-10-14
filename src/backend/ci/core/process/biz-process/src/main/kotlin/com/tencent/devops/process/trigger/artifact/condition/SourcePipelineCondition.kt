package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 防循环条件（强制）：制品的来源流水线 == 当前订阅流水线时不匹配，避免自产自触发。
 */
class SourcePipelineCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val sourcePipelineId = context.factParam.sourcePipelineId
        if (sourcePipelineId.isNullOrBlank() || sourcePipelineId != context.pipelineId) return true
        context.response.failedReason = I18Variable(
            code = WebhookI18nConstants.BK_ARTIFACT_SOURCE_PIPELINE_SELF,
            params = listOf()
        ).toJsonStr()
        return false
    }
}
