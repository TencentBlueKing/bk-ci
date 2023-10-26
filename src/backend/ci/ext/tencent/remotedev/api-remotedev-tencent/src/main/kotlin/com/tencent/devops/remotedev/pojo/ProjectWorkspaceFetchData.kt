package com.tencent.devops.remotedev.pojo

data class ProjectWorkspaceFetchData(
    val projectId: String?,
    val workspaceName: String?,
    val systemType: WorkspaceSystemType?,
    val ips: List<String>?,
    val page: Int?,
    val pageSize: Int?,
    val owner: String?
)
