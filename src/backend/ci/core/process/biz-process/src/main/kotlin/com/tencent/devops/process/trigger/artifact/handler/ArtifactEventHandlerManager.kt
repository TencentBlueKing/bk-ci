package com.tencent.devops.process.trigger.artifact.handler

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactTriggerParam
import org.springframework.stereotype.Service

/**
 * 制品事件处理器管理器：按事件分流到第一个受理的 [ArtifactEventHandler]。
 */
@Service
class ArtifactEventHandlerManager(
    private val handlers: List<ArtifactEventHandler>
) {
    fun getEventDesc(event: ArtifactEvent): I18Variable {
        return resolve(event).getEventDesc(event)
    }

    fun evaluate(
        projectId: String,
        pipelineId: String,
        params: ArtifactTriggerParam,
        event: ArtifactEvent
    ): WebhookMatchResult {
        val handler = handlers.find { it.support(event) }
            ?: return WebhookMatchResult(isMatch = false, reason = "no handler support event")
        return handler.evaluate(
            projectId = projectId,
            pipelineId = pipelineId,
            triggerParams = params,
            event = event
        )
    }

    fun outputs(event: ArtifactEvent): Map<String, Any> {
        return resolve(event).outputs(event)
    }

    private fun resolve(event: ArtifactEvent): ArtifactEventHandler {
        return handlers.find { it.support(event) }
            ?: throw IllegalArgumentException("no handler for ${event::class.simpleName}")
    }
}
