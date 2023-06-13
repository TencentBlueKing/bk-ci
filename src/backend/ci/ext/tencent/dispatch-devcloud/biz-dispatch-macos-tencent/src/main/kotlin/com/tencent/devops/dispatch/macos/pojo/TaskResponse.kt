package com.tencent.devops.dispatch.macos.pojo

import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreateInfo

data class TaskResponse(
    val actionCode: Int,
    val actionMessage: String,
    val data: DevCloudMacosVmCreateInfo
)
