package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud调试登录VM请求")
data class DevCloudMacosVmDebugLoginRequest(
    val taskId: String
)