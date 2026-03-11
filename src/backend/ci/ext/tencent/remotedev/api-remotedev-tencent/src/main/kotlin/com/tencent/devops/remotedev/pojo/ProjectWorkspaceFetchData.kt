package com.tencent.devops.remotedev.pojo

data class ProjectWorkspaceFetchData(
    val projectId: String?,
    val workspaceName: String?,
    val systemType: WorkspaceSystemType?,
    val ips: List<String>?,
    val page: Int?,
    val pageSize: Int?,
    @Deprecated("Replace with owners")
    val owner: String?,
    val owners: Set<String>?,
    val status: WorkspaceStatus?,
    val zoneId: String?,
    val machineType: String?,
    val expertSupId: Long?
)
