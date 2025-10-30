package com.tencent.devops.process.pojo

import com.tencent.devops.process.enums.BuildReplayStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建任务的重放状态
 */
@Schema(title = "构建任务的重放结果")
data class BuildReplayResult(
    @Schema(title = "回放状态")
    val status: BuildReplayStatus,
    @Schema(title = "无效原因")
    val message: String? = null,
    @Schema(title = "回放事件生成的新构建任务构建Id")
    val id: String? = null,
    @Schema(title = "回放webhook事件的触发事件id，用于异步获取构建任务Id")
    val eventId: Long? = null
)
