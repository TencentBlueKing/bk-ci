package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud删除VM")
data class DevCloudWindowsDelete(
    val taskGuid: String
)
