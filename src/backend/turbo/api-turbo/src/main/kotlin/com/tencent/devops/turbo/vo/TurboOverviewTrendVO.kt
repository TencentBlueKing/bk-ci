package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("总览页趋势图视图")
data class TurboOverviewTrendVO(

    @ApiModelProperty("日期")
    val date: String = "",

    @ApiModelProperty("编译次数")
    val executeCount: Int = 0,

    @ApiModelProperty("实际耗时")
    val executeTime: Double = 0.0,

    @ApiModelProperty("未加速耗时")
    val estimateTime: Double = 0.0
)
