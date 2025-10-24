package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建任务的重放状态
 */
@Schema(title = "构建任务的重放状态")
data class BuildReplayStatus(
    @Schema(title = "是否可重放")
    val status: Boolean,
    @Schema(title = "无效原因")
    val message: String = ""
)
