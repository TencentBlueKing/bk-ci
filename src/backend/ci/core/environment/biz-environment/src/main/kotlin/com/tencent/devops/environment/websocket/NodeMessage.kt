package com.tencent.devops.environment.websocket

import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.pojo.NotifyPost

data class NodeMessage(
		val project: String?,
		override val userId: String,
		override val sessionList: List<String>?,
		override var page: String?,
		override var notifyPost: NotifyPost
): SendMessage(userId, page, sessionList, notifyPost)