package com.tencent.devops.common.websocket.pojo

data class NotifyPost(
    var module: String,
    var level: Int,
    var code: Int,
    var message: String,
    var dealUrl: String?,
    var webSocketType: String?
)