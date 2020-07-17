package com.tencent.devops.sign.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("APPEX拓展信息")
data class AppexSignInfo(
    @ApiModelProperty("appex拓展应用名", required = true)
    val appexName: String,
    @ApiModelProperty("扩展App对应描述文件ID", required = true)
    val mobileProvisionId: String
)