package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "SSH公钥")
data class SshPublicKey(
    @Schema(description = "用户")
    val user: String,
    @Schema(description = "公钥")
    val publicKey: String
)
