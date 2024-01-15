package com.tencent.devops.dispatch.docker.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "devcloud性能用户选项配置")
data class UserPerformanceOptionsVO(
    @Schema(name = "默认选项")
    val default: String,
    @Schema(name = "是否展示")
    val needShow: Boolean,
    @Schema(name = "性能配置")
    val performanceMaps: List<PerformanceMap>
)

data class PerformanceMap(
    val id: String,
    val performanceConfigVO: PerformanceConfigVO
)
