package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-每日拦截")
data class CountDailyIntercept(
    @ApiModelProperty("日期", required = true)
    val date: String,
    @ApiModelProperty("拦截数", required = true)
    val count: Int,
    @ApiModelProperty("生效流水线执行数", required = true)
    val pipelineExecuteCount: Int = 0
)