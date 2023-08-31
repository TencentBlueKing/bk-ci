package com.tencent.devops.dispatch.kubernetes.pojo

data class CreateWorkspaceRes(
    val enviromentUid: String,
    val taskId: String,
    val regionId: Int = 0,
    val resourceId: String? = null
)
