package com.tencent.devops.common.event.pojo.pipeline

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType

@Event(MQ.EXCHANGE_PIPELINE_BUILD_START_FANOUT, MQ.ROUTE_PIPELINE_BUILD_START)
data class PipelineBuildStartBroadCastEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    override var actionType: ActionType = ActionType.REFRESH,
    override var delayMills: Int = 0,
    val startTime: Long?,
    val triggerType: String
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
