package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-把关操作记录")
data class QualityRuleBuildHisOpt(
    @ApiModelProperty("红线hashId", required = true)
    val ruleHashId: String,
    @ApiModelProperty("红线把关人", required = false)
    val gateKeepers: List<String>? = null,
    @ApiModelProperty("stageId", required = false)
    val stageId: String? = "",
    @ApiModelProperty("操作人", required = false)
    val gateOptUser: String? = "",
    @ApiModelProperty("操作时间", required = false)
    val gateOptTime: String? = ""
)
