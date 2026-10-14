package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 监听流水线条件：watchPipeline 必须非空，且制品生产流水线必须等于该值。
 */
class WatchPipelineCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val watch = context.triggerParam.watchPipeline
        if (!watch.isNullOrBlank() && context.factParam.sourcePipelineId == watch) return true
        context.response.failedReason = I18Variable(
            code = WebhookI18nConstants.BK_ARTIFACT_WATCH_PIPELINE_NOT_MATCH,
            params = listOf()
        ).toJsonStr()
        return false
    }
}
