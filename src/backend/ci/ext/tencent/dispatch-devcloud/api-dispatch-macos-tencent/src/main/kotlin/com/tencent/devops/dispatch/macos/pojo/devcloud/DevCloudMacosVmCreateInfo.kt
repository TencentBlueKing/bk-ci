package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM信息")
data class DevCloudMacosVmCreateInfo(
    var creator: String,
    var name: String,
    var memory: String,
    var assetId: String,
    var ip: String,
    var disk: String,
    var os: String,
    var id: Int,
    var createdAt: String,
    var cpu: String,
    var user: String = "",
    var password: String = ""
)
