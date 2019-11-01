package com.tencent.devops.websocket.pojo

data class LoginOutDTO(
    val userId: String,
    val sessionId: String,
    val page: String? = null
)