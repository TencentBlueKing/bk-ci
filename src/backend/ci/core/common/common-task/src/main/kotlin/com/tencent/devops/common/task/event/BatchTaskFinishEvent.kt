package com.tencent.devops.common.task.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Event(destination = StreamBinding.PIPELINE_BATCH_ARCHIVE_FINISH)
data class BatchTaskFinishEvent(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "任务类型")
    val taskType: TaskTypeEnum,
    @get:Schema(title = "批次ID")
    val batchId: String,
    @get:Schema(title = "目标微服务名称")
    val targetService: String? = null
) : IEvent()
