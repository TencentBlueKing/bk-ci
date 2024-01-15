package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devcloud性能基础选项配置")
data class PerformanceOptionsVO(
    @Schema(description = "CPU")
    val cpu: Int,
    @Schema(description = "内存")
    val memory: Int,
    @Schema(description = "磁盘")
    val disk: Int,
    @Schema(description = "描述")
    val description: String
)
