package com.tencent.devops.remotedev.pojo.op

import io.swagger.annotations.ApiParam

data class WorkspaceNotifyData(
    @ApiParam(value = "ip", required = true)
    val ip: String,
    @ApiParam(value = "content", required = true)
    val content: String
)
