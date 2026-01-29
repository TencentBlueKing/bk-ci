package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud调试登录VM信息")
data class DevCloudMacosVmDebugLoginInfo(
    val status: String = "",
    val ip: String = "",
    val user: String = "",
    val password: String = "",
    val cpu: String = "",
    val memory: String = "",
    val disk: String = "",
    val id: Int = 0,
    val createdAt: String = "",
    val assetId: String = "",
    val os: String = "",
    val creator: String = "",
    val name: String = "",
    val taskId: String = ""
)