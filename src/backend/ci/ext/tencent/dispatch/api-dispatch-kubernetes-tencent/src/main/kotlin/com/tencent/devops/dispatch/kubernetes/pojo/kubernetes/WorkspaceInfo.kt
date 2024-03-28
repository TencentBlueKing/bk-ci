package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

import io.swagger.v3.oas.annotations.media.Schema

data class WorkspaceInfo(
    val status: EnvStatusEnum,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String,
    val namespace: String,
    val environmentHost: String,
    @get:Schema(title = "对应pod是否可用，可能为null")
    val ready: Boolean?,
    @get:Schema(title = "对应pod是否可用，可能为null")
    val started: Boolean?,
    @get:Schema(title = "start 云桌面使用")
    val curLaunchId: Int? = null,
    @get:Schema(title = "云区域ID，start 云桌面使用")
    val regionId: Int? = null
)
