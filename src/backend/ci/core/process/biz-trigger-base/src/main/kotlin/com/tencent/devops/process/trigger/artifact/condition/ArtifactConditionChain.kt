package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 制品触发条件链（责任链）。
 *
 * 逐个执行条件，任一不通过即失败并返回 [ArtifactConditionContext.response] 中的失败原因；
 * 全部通过则返回累积的输出参数。
 */
class ArtifactConditionChain(
    private val conditions: List<ArtifactCondition>
) {
    fun match(context: ArtifactConditionContext): WebhookMatchResult {
        val matched = conditions.all { it.match(context) }
        return if (matched) {
            WebhookMatchResult(isMatch = true, extra = context.response.params.toMap())
        } else {
            WebhookMatchResult(isMatch = false, reason = context.response.failedReason)
        }
    }
}
