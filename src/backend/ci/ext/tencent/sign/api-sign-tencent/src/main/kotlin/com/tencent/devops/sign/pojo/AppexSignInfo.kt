package com.tencent.devops.sign.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目信息")
data class AppexSignInfo(
    @ApiModelProperty("appex拓展应用名", required = true)
    val appexName: String,
    @ApiModelProperty("对应证书ID", required = true)
    val certId: String
)