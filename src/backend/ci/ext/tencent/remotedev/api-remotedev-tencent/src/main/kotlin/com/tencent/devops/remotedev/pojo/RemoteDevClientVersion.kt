package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "客户端版本配置")
data class RemoteDevClientVersion(
    @Schema(description = "环境 gray or prod")
    val env: String,
    @Schema(description = "版本")
    val version: String
)
