package com.tencent.devops.common.websocket.dispatch.message

import com.tencent.devops.common.websocket.pojo.NotifyPost

data class NotifyMessage(
    val buildId: String?,
    val projectId: String,
    val pipelineId: String,
    override val userId: String,
    override val sessionList: List<String>,
    override var page: String?,
    override var notifyPost: NotifyPost
) : SendMessage(userId, page, sessionList, notifyPost)