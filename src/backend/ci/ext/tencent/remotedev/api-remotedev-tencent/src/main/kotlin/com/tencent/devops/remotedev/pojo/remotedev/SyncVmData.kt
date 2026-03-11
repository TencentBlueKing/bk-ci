package com.tencent.devops.remotedev.pojo.remotedev

data class SyncVmData(
    val syncOnly: Boolean?,
    val targetWorkspaceName: String,
    val sourceWorkspaceName: String
)

data class SyncVmInfo(
    val userId: String,
    val syncOnly: Boolean?,
    val targetWorkspaceName: String,
    val sourceWorkspaceName: String
)

data class SyncVmResp(
    val environmentUid: String?,
    val taskID: String,
    val taskUid: String
)