package com.tencent.devops.dispatch.macos.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "vm机器密码-凭据内容")
data class PasswordInfo(
    @get:Schema(title = "Base64编码的加密公钥", required = true)
    val publicKey: String,
    @get:Schema(title = "加密后经过Base64编码的凭据", required = true)
    val password: String
)
