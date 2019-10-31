package com.tencent.devops.websocket.pojo

data class ChangePageDTO(
    val userId: String,
    val sessionId: String,
    val page: String,
    val projectId: String
)