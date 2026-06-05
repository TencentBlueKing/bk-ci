package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud关闭调试登录VM信息")
data class DevCloudMacosVmDebugCloseInfo(
    @Schema(title = "任务ID")
    val taskId: String = ""
)
