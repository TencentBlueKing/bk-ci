package com.tencent.devops.dispatch.windows.pojo

import io.swagger.annotations.ApiModel

@ApiModel("DevCloud创建VM信息")
data class DevCloudWindowsCreateInfo(
    val taskStatus: String,
    val ip: String,
    val ipStatus: String,
    val processId: String,
    val buildTime: String,
    val createdAt: String,
    val updatedAt: String,
    val taskGuid: String
)
