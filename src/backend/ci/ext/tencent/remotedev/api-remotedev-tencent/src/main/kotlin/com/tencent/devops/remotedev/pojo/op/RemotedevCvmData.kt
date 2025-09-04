package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter

data class RemotedevCvmData(
    @get:Schema(title = "ID")
    val id: Int?,
    @Parameter(description = "项目ID", required = true)
    val projectId: String,
    @Parameter(description = "区域", required = true)
    val zone: String?,
    @Parameter(description = "可用区域", required = true)
    val availableRegion: String?,
    @Parameter(description = "cpu", required = true)
    val cpu: Int?,
    @Parameter(description = "内存", required = true)
    val memory: Int?,
    @Parameter(description = "子网", required = true)
    val subnet: String?,
    @Parameter(description = "云桌面IP", required = true)
    val ip: String
)
