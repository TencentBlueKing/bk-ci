package com.tencent.devops.scm.code.p4.api

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("p4服务端信息")
data class P4ServerInfo(
    @ApiModelProperty("区别大小写")
    val caseSensitive: Boolean
)
