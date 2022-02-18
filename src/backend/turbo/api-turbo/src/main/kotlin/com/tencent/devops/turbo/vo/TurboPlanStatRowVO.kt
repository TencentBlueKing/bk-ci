package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("加速方案-列表页,方案清单视图")
data class TurboPlanStatRowVO(
    @ApiModelProperty("方案id")
    val planId: String? = "",

    @ApiModelProperty("是否置顶")
    val topStatus: String? = "",

    @ApiModelProperty("方案名称")
    val planName: String? = "",

    @ApiModelProperty("蓝盾模板代码")
    val engineCode: String? = "",

    @ApiModelProperty("蓝盾模板名称")
    val engineName: String? = "",

    @ApiModelProperty("加速方案数量")
    val instanceNum: Int? = 0,

    @ApiModelProperty("加速次数")
    val executeCount: Int? = 0,

    @ApiModelProperty("未加速耗时")
    val estimateTimeHour: String? = "",

    @ApiModelProperty("实际耗时")
    val executeTimeHour: String? = "",

    @ApiModelProperty("节省率")
    val turboRatio: String? = "--",

    @ApiModelProperty("项目状态")
    val openStatus: Boolean? = true
)
