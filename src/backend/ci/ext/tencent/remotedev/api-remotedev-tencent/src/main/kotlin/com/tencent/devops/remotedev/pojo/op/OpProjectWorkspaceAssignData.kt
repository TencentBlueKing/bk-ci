package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.Parameter

data class OpProjectWorkspaceAssignData(
    @Parameter(description = "项目ID", required = true)
    val projectId: String,
    @Parameter(description = "云桌面ID", required = true)
    val cgsIds: List<String>?,
    @Parameter(description = "云桌面IP", required = true)
    val ips: List<String>?,
    @Parameter(description = "仓库ID")
    val repoId: String?,
    @Parameter(description = "云桌面盘符")
    val localDriver: String?
)
