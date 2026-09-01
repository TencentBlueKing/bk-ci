package com.tencent.devops.remotedev.pojo

data class WorkspaceRecordInfo(
    val projectId: String,
    val workspaceName: String,
    val coffeeAIEnable: Boolean,
    val enableUser: String?,
    val hostIp: String,
)
