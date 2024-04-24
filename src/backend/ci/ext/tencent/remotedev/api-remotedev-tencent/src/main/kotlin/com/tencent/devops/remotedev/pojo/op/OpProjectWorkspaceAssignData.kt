package com.tencent.devops.remotedev.pojo.op

import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import io.swagger.v3.oas.annotations.Parameter

data class OpProjectWorkspaceAssignData(
    @Parameter(description = "分配给项目", required = false)
    val projectId: String?,
    @Parameter(description = "分配给个人", required = false)
    val owner: String?,
    @Parameter(description = "云桌面ID", required = false)
    val cgsIds: List<String>?,
    @Parameter(description = "云桌面IP", required = false)
    val ips: List<String>?,
    @Parameter(description = "仓库ID")
    val repoId: String?,
    @Parameter(description = "云桌面盘符")
    val localDriver: String?,
    @Parameter(description = "分配类型")
    val type: WorkspaceOwnerType = WorkspaceOwnerType.PROJECT
)
