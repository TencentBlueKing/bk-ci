package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class CreateWorkspaceRes(
    val enviromentUid: String,
    val taskUid: String,
    val taskId: String
)
