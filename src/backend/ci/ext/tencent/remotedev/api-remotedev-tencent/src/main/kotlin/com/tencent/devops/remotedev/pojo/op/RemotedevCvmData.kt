package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter

data class RemotedevCvmData(
    @get:Schema(title = "ID")
    val id: Int?,
    @Parameter(name = "项目ID", required = true)
    val projectId: String,
    @Parameter(name = "区域", required = true)
    val zone: String?,
    @Parameter(name = "可用区域", required = true)
    val availableRegion: String?,
    @Parameter(name = "cpu", required = true)
    val cpu: Int?,
    @Parameter(name = "内存", required = true)
    val memory: Int?,
    @Parameter(name = "子网", required = true)
    val subnet: String?,
    @Parameter(name = "云桌面IP", required = true)
    val ip: String
)
