package com.tencent.devops.dispatch.kubernetes.pojo

import java.time.LocalDateTime

data class DispatchWorkspaceOpHisRecord(
    val workspaceName: String,
    val envId: String,
    val operator: String,
    val action: EnvironmentAction,
    val actionMsg: String,
    val createTime: LocalDateTime,
    val uid: String,
    val status: EnvironmentActionStatus
)
