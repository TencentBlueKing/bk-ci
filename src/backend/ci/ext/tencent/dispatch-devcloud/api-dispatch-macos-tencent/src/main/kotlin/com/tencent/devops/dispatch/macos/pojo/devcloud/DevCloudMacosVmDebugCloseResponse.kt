package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud关闭调试登录VM响应")
data class DevCloudMacosVmDebugCloseResponse(
    val actionCode: Int,
    val actionMessage: String,
    val data: DevCloudMacosVmDebugCloseInfo? = null
)
