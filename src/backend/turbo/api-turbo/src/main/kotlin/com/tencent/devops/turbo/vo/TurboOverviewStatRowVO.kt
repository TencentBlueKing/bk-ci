package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("总览页导航栏视图")
data class TurboOverviewStatRowVO(
    @ApiModelProperty("加速方案数量")
    val instanceNum: Int = 0,

    @ApiModelProperty("加速次数")
    val executeCount: Int = 0,

    @ApiModelProperty("总耗时")
    val executeTimeHour: String = "",

    @ApiModelProperty("节省率")
    val savingRate: String = ""
)
