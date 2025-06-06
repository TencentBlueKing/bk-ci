package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

data class SyncVmReq(
    val syncOnly: Boolean?,
    val targetEnvID: String,
    val uid: String
)