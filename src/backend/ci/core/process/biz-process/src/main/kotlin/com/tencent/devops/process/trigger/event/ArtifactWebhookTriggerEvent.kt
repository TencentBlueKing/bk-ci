package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品到达 Webhook 单流水线触发事件
 *
 * 参考 [TapdWebhookTriggerEvent]：只投递「路由 + 事件标识」，
 * 构建端 [com.tencent.devops.process.trigger.artifact.ArtifactEventTriggerBuildService]
 * 通过 eventId 反查 [com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent.eventBody] 拿到原始 body 再匹配。
 */
@Event(StreamBinding.ARTIFACT_WEBHOOK_TRIGGER_EVENT)
@Schema(title = "制品到达 webhook 单流水线触发事件")
data class ArtifactWebhookTriggerEvent(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "事件ID")
    val eventId: Long,
    @get:Schema(title = "事件源 (projectId:pipeline / projectId:custom / projectId:image)")
    val eventSource: String,
    @get:Schema(title = "制品事件类型")
    val eventType: String,
    @get:Schema(title = "触发用户")
    val triggerUser: String
) : IEvent()
