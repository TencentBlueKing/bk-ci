package com.tencent.devops.dispatch.docker.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "devcloud项目性能配置")
data class PerformanceConfigVO(
    @Schema(name = "蓝盾项目ID")
    val projectId: String,
    @Schema(name = "CPU")
    val cpu: Int,
    @Schema(name = "内存")
    val memory: String,
    @Schema(name = "磁盘")
    val disk: String,
    @Schema(name = "描述")
    val description: String
)
