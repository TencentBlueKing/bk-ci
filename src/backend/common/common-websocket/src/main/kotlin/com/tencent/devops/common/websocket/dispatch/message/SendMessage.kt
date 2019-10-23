package com.tencent.devops.common.websocket.dispatch.message

import com.tencent.devops.common.websocket.pojo.NotifyPost

abstract class SendMessage(
    open val userId: String,
    open var page: String?,
    open val sessionList: List<String>?,
    open var notifyPost: NotifyPost
)