package com.tencent.devops.dispatch.docker.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devcloud项目性能配置")
data class PerformanceConfigVO(
    @Schema(description = "蓝盾项目ID")
    val projectId: String,
    @Schema(description = "CPU")
    val cpu: Int,
    @Schema(description = "内存")
    val memory: String,
    @Schema(description = "磁盘")
    val disk: String,
    @Schema(description = "描述")
    val description: String
)
