package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiParam
import javax.ws.rs.QueryParam

data class ProjectWorkspaceFetchData(
    val projectId: String?,
    val workspaceName: String?,
    val systemType: WorkspaceSystemType?,
    val ips: List<String>?,
    val page: Int?,
    val pageSize: Int?
)
