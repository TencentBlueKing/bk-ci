package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devcloud项目性能配置")
data class OPPerformanceConfigVO(
    @Schema(description = "蓝盾项目ID")
    val projectId: String,
    @Schema(description = "性能基础配置ID")
    val optionId: Long
)
