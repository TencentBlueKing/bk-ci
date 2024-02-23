package com.tencent.devops.remotedev.pojo.op

import io.swagger.v3.oas.annotations.Parameter

data class WorkspaceNotifyData(
    @Parameter(name = "projectId", required = true)
    val projectId: List<String>?,
    @Parameter(name = "ip", required = false)
    val ip: List<String>?,
    @Parameter(name = "title", required = true)
    val title: String,
    @Parameter(name = "desc", required = false)
    val desc: String?
)
