package com.tencent.devops.common.websocket.dispatch.message

import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType

abstract class MqMessage(
    open val userId: String,
    open val pushType: WebSocketType,
    open var page: String?,
    open var notifyPost: NotifyPost
)