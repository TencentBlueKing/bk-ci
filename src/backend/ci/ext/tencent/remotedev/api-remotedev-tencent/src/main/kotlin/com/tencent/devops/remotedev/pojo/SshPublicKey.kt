package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "SSH公钥")
data class SshPublicKey(
    @Schema(title = "用户")
    val user: String,
    @Schema(title = "公钥")
    val publicKey: String
)
