package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间信息缓存")
data class WorkSpaceCacheInfo(
    @get:Schema(title = "工作空间关联秘钥")
    val sshKey: String,
    @get:Schema(title = "工作空间Host")
    val environmentHost: String,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String,
    val namespace: String,
    val curLaunchId: Int?,
    val regionId: Int?
)
