package com.tencent.devops.common.pipeline.pojo.coverity

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Codecc-Report信息")
data class CodeccReport(
    @ApiModelProperty("codecc report信息", required = true)
    val report: String
)