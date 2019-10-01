package com.tencent.devops.quality.api.v2.pojo.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-总览v2")
data class CountOverviewResponse(
    @ApiModelProperty("规则数", required = true)
    val ruleCount: Int,
    @ApiModelProperty("指标数", required = true)
    val indicatoCount: Int,
    @ApiModelProperty("拦截数", required = true)
    val interceptCount: Int,
    @ApiModelProperty("流水线数", required = true)
    val pipelineCount: Int
)