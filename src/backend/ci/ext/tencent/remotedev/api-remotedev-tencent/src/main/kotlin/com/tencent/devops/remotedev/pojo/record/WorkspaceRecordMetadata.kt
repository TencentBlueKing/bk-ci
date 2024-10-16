package com.tencent.devops.remotedev.pojo.record

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间录屏元数据")
data class WorkspaceRecordMetadata(
    @get:Schema(title = "录屏链接", required = true)
    val link: String,
    @get:Schema(title = "开始时间", required = true)
    val startTime: Long?,
    @get:Schema(title = "停止时间", required = true)
    val stopTime: Long?
)
