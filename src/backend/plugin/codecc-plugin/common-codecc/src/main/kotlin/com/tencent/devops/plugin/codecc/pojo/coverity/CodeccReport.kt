package com.tencent.devops.plugin.codecc.pojo.coverity

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Codecc-Report信息")
data class CodeccReport(
    @ApiModelProperty("codecc report信息", required = true)
    val report: String
)