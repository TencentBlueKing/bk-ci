package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装IDE插件请求报文")
data class InstallIdeAtomReq(
    @ApiModelProperty("插件标识", required = true)
    val atomCode: String
)