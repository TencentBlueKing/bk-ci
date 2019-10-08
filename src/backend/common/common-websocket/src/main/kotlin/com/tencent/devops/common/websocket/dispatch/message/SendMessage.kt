package com.tencent.devops.common.websocket.dispatch.message

import com.tencent.devops.common.websocket.pojo.NotifyPost

class SendMessage(
        val userId: String,
        var page: String?,
        var associationPage: List<String>,
        var notifyPost: NotifyPost
)