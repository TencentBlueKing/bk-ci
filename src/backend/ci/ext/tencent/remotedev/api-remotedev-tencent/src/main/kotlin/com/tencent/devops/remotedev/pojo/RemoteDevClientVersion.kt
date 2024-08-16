package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "客户端版本配置")
data class RemoteDevClientVersion(
    @get:Schema(title = "环境 gray or prod")
    val env: String,
    @get:Schema(title = "版本")
    val version: String
)
