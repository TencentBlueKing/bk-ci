package com.tencent.devops.project.pojo.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.stream.constants.StreamBinding
import io.swagger.v3.oas.annotations.media.Schema

@Event(StreamBinding.PROJECT_ENABLE_FANOUT)
data class ProjectEnableStatusBroadCastEvent(
    override val userId: String,
    override val projectId: String,
    override var retryCount: Int = 0,
    override var delayMills: Int = 0,
    @get:Schema(title = "是否启用")
    val enabled: Boolean
) : ProjectBroadCastEvent(userId, projectId, retryCount, delayMills)
