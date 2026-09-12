package com.tencent.devops.process.trigger.artifact.pojo

import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse

/**
 * 制品触发条件链上下文。
 *
 * - [projectId]：订阅事件的项目ID；
 * - [pipelineId]：订阅事件的流水线ID（用于防循环）；
 * - [triggerParam]：用户在插件里填写的触发配置（变量替换后）；
 * - [factParam]：从制品事件中抽取的实际事实；
 * - [response]：命中过程中的输出参数与失败原因载体（复用 webhook 过滤器时写入）。
 */
class ArtifactConditionContext(
    val projectId: String,
    val pipelineId: String,
    val triggerParam: ArtifactTriggerParam,
    val factParam: ArtifactFactParam,
    val response: WebhookFilterResponse = WebhookFilterResponse()
)
