package com.tencent.devops.plugin.pojo.tcm

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("tcm应用类型")
data class TcmApp(
    @ApiModelProperty("应用id")
    val buid: String,
    @ApiModelProperty("应用名称")
    val buname: String
)