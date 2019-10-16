package com.tencent.devops.process.pojo.third.tcls

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TCLS 环境")
data class TclsEnv(
    @ApiModelProperty(value = "环境 ID", required = true)
    val envId: String,
    @ApiModelProperty(value = "环境名称", required = true)
    val envName: String
)