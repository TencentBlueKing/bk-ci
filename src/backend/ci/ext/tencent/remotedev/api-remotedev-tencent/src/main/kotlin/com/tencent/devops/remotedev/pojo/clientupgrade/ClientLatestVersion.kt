package com.tencent.devops.remotedev.pojo.clientupgrade

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "客户端最新版本信息")
data class ClientLatestVersion(
    @get:Schema(title = "最新版本号，未设置则为空")
    val version: String?
)
