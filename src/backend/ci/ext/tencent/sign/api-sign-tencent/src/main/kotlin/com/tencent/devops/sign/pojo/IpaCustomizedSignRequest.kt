package com.tencent.devops.sign.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IPA包签名请求")
data class IpaCustomizedSignRequest(
    @ApiModelProperty("keystore应用ID", required = false)
    var appId: String? = null,
    @ApiModelProperty("拓展应用名和对应的描述文件ID", required = false)
    var appexSignInfo: List<AppexSignInfo>? = null
)