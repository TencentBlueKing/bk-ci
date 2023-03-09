package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM")
data class DevCloudWindowsCreate(
    var regionId: String? = "ap-guangzhou",
    var os: String? = "windows-2022",
    var cpu: String = "8",
    var memory: String = "16",
    var disk: String = "100",
    var env: DevCloudWindowsCreateEnv
)
