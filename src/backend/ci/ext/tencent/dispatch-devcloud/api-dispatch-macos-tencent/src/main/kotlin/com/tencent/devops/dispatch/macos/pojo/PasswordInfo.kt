package com.tencent.devops.dispatch.macos.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("vm机器密码-凭据内容")
data class PasswordInfo(
    @ApiModelProperty("Base64编码的加密公钥", required = true)
    val publicKey: String,
    @ApiModelProperty("加密后经过Base64编码的凭据", required = true)
    val password: String
)
