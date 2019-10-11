package com.tencent.devops.plugin.pojo.ons

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ons名字信息")
data class OnsNameInfo(
    @ApiModelProperty("IP地址")
    val ip: String,
    @ApiModelProperty("端口")
    val port: Int
)