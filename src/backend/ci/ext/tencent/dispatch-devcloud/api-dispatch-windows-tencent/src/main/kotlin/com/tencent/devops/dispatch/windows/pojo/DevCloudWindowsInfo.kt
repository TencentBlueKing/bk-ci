package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM信息")
data class DevCloudWindowsInfo(
    var regionId: String,
    var os: String,
    var cpu: String,
    var memory: String,
    var disk: String,
    var assetId: String,
    var ip: String,
    var id: Int
)
