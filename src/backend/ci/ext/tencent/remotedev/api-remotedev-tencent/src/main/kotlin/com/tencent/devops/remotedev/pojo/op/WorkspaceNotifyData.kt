package com.tencent.devops.remotedev.pojo.op

import io.swagger.annotations.ApiParam

data class WorkspaceNotifyData(
    @ApiParam(value = "projectId", required = true)
    val projectId: String,
    @ApiParam(value = "ip", required = false)
    val ip: List<String>?,
    @ApiParam(value = "title", required = true)
    val title: String,
    @ApiParam(value = "desc", required = false)
    val desc: String?
)
