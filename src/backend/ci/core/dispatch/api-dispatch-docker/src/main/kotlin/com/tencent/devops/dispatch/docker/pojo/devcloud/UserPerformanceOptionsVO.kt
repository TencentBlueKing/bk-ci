package com.tencent.devops.dispatch.docker.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devcloud性能用户选项配置")
data class UserPerformanceOptionsVO(
    @Schema(description = "默认选项")
    val default: String,
    @Schema(description = "是否展示")
    val needShow: Boolean,
    @Schema(description = "性能配置")
    val performanceMaps: List<PerformanceMap>
)

data class PerformanceMap(
    val id: String,
    val performanceConfigVO: PerformanceConfigVO
)
