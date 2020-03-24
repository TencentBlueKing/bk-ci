package com.tencent.devops.project.pojo.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.project.pojo.ProjectEnableInfo

@Event(MQ.EXCHANGE_PROJECT_ENABLE_FANOUT)
data class ProjectEnableBroadCastEvent(
    override val userId: String,
    override val projectId: String,
    override var retryCount: Int = 0,
    override var delayMills: Int = 0,
    val projectEnableInfo: ProjectEnableInfo
) : ProjectBroadCastEvent(userId, projectId, retryCount, delayMills)