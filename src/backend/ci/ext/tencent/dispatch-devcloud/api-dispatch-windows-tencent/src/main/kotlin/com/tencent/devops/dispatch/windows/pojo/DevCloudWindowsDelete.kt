package com.tencent.devops.dispatch.windows.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud删除VM")
data class DevCloudWindowsDelete(
    val taskGuid: String
)
