package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务创建事件")
@Event(StreamBinding.PIPELINE_BATCH_TASK_CREATE)
data class PipelineBatchTaskCreateEvent(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "任务类型", required = true)
    val taskType: PipelineBatchTaskType,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String
) : IEvent()
