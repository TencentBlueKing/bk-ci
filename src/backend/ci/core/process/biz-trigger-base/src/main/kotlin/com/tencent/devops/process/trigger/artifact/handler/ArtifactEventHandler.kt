package com.tencent.devops.process.trigger.artifact.handler

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactTriggerParam

/**
 * 制品事件处理器
 *
 * 每个实现负责一类事件（节点 / 包版本）：先 [support] 判定是否受理，
 * 再分别提供事件描述、匹配评估和事件输出。
 */
interface ArtifactEventHandler {
    fun support(event: ArtifactEvent): Boolean

    fun getEventDesc(event: ArtifactEvent): I18Variable

    fun evaluate(
        projectId: String,
        pipelineId: String,
        triggerParams: ArtifactTriggerParam,
        event: ArtifactEvent
    ): WebhookMatchResult

    fun outputs(event: ArtifactEvent): Map<String, Any>
}
