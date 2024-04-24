package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "SSH公钥")
data class SshPublicKey(
    @get:Schema(title = "用户")
    val user: String,
    @get:Schema(title = "公钥")
    val publicKey: String
)
