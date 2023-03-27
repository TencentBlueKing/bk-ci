package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM信息")
data class DevCloudMacosVmInfo(
    var name: String,
    var memory: String,
    var assetId: String,
    var ip: String,
    var disk: String,
    var os: String,
    var id: Int,
    var cpu: String
)
