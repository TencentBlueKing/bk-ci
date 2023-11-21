package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

data class WorkspaceResponse(
    val environmentHost: String,
    val environmentUid: String,
    val environmentIp: String,
    val resourceId: String? = null,
    val macAddress: String? = null
)
