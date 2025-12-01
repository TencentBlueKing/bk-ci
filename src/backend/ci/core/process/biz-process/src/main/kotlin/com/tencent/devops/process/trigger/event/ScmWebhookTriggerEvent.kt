package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.repository.pojo.Repository
import io.swagger.v3.oas.annotations.media.Schema

@Event(StreamBinding.SCM_HOOK_BUILD_TRIGGER_EVENT)
@Schema(title = "scm webhook触发事件")
data class ScmWebhookTriggerEvent(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线版本", required = true)
    val version: Int?,
    @get:Schema(title = "事件ID", required = true)
    val eventId: Long,
    @get:Schema(title = "代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "webhook请求时间", required = true)
    val requestTime: Long
) : IEvent()
