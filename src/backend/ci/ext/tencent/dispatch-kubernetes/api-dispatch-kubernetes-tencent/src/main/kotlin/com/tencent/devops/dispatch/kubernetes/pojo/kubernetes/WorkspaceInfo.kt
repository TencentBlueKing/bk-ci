package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

import io.swagger.annotations.ApiModelProperty

data class WorkspaceInfo(
    val status: EnvStatusEnum,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String,
    val namespace: String,
    val environmentHost: String,
    @ApiModelProperty("对应pod是否可用，可能为null")
    val ready: Boolean?,
    @ApiModelProperty("对应pod是否可用，可能为null")
    val started: Boolean?
)
