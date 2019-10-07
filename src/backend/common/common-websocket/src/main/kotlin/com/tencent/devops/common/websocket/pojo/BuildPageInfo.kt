package com.tencent.devops.common.websocket.pojo

data class BuildPageInfo(
    val buildId: String?,
    val pipelineId: String?,
    val projectId: String?,
    val atomId: String?
)