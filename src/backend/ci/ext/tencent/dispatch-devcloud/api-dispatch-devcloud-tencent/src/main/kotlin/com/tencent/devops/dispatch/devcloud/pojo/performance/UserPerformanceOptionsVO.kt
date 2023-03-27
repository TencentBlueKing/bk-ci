package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devcloud性能用户选项配置")
data class UserPerformanceOptionsVO(
    @ApiModelProperty("默认选项")
    val default: String,
    @ApiModelProperty("是否展示")
    val needShow: Boolean,
    @ApiModelProperty("性能配置")
    val performanceMaps: List<PerformanceMap>
)

data class PerformanceMap(
    val id: String,
    val performanceConfigVO: PerformanceConfigVO
)
