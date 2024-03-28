package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.Parameter

data class WorkspaceNotifyData(
    @Parameter(description = "projectId", required = true)
    val projectId: List<String>?,
    @Parameter(description = "ip", required = false)
    val ip: List<String>?,
    @Parameter(description = "title", required = true)
    val title: String,
    @Parameter(description = "desc", required = false)
    val desc: String?
)
