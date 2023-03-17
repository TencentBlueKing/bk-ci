package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("SSH公钥")
data class SshPublicKey(
    @ApiModelProperty("用户")
    val user: String,
    @ApiModelProperty("公钥")
    val publicKey: String
)
