package com.tencent.devops.common.websocket.pojo

data class MessageInfo(
    val pipelineId: String?,
    var buildId: String?,
    val userId: String,
    val type: WebSocketType,
    val projectId: String?,
    val notifyPost: NotifyPost?,
    val page: String,
    val atomId: String? = null
)