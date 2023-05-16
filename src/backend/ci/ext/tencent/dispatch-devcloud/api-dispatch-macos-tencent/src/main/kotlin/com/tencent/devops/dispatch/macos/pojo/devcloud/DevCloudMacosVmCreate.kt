package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM")
data class DevCloudMacosVmCreate(
    var project: String,
    var pipelineId: String,
    var buildId: String,
    var vmSeqId: String,
    var source: String = "",
    var os: String? = "Bigsur 11.4",
    var xcode: String? = "13.2.1",
    var cpu: String = "8",
    var memory: String = "16",
    var disk: String = "800",
    var quantity: Int = 1
)
