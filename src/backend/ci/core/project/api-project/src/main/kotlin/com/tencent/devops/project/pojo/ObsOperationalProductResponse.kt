package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("OBS运营产品")
data class ObsOperationalProductResponse(
    @ApiModelProperty("jsonrpc")
    val jsonrpc: String,
    @ApiModelProperty("id")
    val id: String,
    @ApiModelProperty("运营产品")
    val result: ObsOperationalProductResult
)
