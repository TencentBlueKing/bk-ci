package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM")
data class DevCloudMacosVmCreate(
    val project: String,
    val pipelineId: String,
    val buildId: String,
    val vmSeqId: String,
    val source: String = "",
    val os: String? = "Bigsur 11.4",
    val xcode: String? = "13.2.1",
    val cpu: String = "8",
    val memory: String = "16",
    val disk: String = "800",
    val quantity: Int = 1,
    val env: Map<String, String> = emptyMap()
)
