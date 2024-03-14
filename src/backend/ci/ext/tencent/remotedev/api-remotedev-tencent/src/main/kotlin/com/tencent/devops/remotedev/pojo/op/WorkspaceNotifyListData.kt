package com.tencent.devops.remotedev.pojo.op

data class WorkspaceNotifyListData(
    val projectId: String,
    val ip: String,
    val title: String,
    val desc: String,
    val createTime: String,
    val operator: String? = ""
)
