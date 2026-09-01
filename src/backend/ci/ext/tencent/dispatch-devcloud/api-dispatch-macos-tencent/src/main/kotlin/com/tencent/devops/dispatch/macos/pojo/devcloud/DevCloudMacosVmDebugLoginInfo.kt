package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud调试登录VM信息")
data class DevCloudMacosVmDebugLoginInfo(
    @Schema(title = "VNC连接地址")
    val vnc: String = "",
    @Schema(title = "SSH连接地址")
    val ssh: String = "",
    @Schema(title = "登录用户名")
    val username: String = "",
    @Schema(title = "登录密码")
    val password: String = ""
)