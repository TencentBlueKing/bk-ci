package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("客户端版本配置")
data class RemoteDevClientVersion(
    @ApiModelProperty("环境 gray or prod")
    val env: String,
    @ApiModelProperty("版本")
    val version: String
)
