package com.tencent.devops.common.task.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Event(destination = StreamBinding.PIPELINE_BATCH_ARCHIVE_PUBLISH)
data class BatchTaskPublishEvent(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "任务类型")
    val taskType: TaskTypeEnum,
    @get:Schema(title = "批次ID")
    val batchId: String,
    @get:Schema(title = "任务ID")
    val taskId: String,
    @get:Schema(title = "业务数据")
    val data: Map<String, Any>,
    @get:Schema(title = "过期时间（单位：小时，默认12小时）")
    var expiredInHour: Long = 12,
    @get:Schema(title = "目标微服务名称")
    val targetService: String? = null
) : IEvent()
