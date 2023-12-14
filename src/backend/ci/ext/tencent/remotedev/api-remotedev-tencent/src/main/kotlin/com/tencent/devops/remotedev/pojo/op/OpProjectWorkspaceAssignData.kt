package com.tencent.devops.remotedev.pojo.op

import io.swagger.annotations.ApiParam

data class OpProjectWorkspaceAssignData(
    @ApiParam(value = "项目ID", required = true)
    val projectId: String,
    @ApiParam(value = "云桌面ID", required = true)
    val cgsIds: List<String>?,
    @ApiParam(value = "云桌面IP", required = true)
    val ips: List<String>?,
    @ApiParam("仓库ID")
    val repoId: String?,
    @ApiParam("云桌面盘符")
    val localDriver: String?
)
