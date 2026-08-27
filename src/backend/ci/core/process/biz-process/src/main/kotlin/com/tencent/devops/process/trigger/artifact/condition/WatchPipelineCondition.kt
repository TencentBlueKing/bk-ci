package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 监听流水线条件：triggerParams.watchPipeline 非空时，制品生产流水线必须命中其中之一；为空则不限定来源流水线。
 */
class WatchPipelineCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val watch = context.triggerParam.watchPipeline
        if (watch.isEmpty()) return true
        val source = context.factParam.sourcePipelineId
        if (source != null && source in watch) return true
        context.response.failedReason = I18Variable(
            code = WebhookI18nConstants.BK_ARTIFACT_WATCH_PIPELINE_NOT_MATCH,
            params = listOf()
        ).toJsonStr()
        return false
    }
}
