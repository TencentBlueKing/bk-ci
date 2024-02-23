package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.Parameter

data class OpProjectWorkspaceAssignData(
    @Parameter(name = "项目ID", required = true)
    val projectId: String,
    @Parameter(name = "云桌面ID", required = true)
    val cgsIds: List<String>?,
    @Parameter(name = "云桌面IP", required = true)
    val ips: List<String>?,
    @Parameter(name = "仓库ID")
    val repoId: String?,
    @Parameter(name = "云桌面盘符")
    val localDriver: String?
)
