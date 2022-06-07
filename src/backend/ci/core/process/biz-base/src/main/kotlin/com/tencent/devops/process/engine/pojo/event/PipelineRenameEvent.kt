package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent

@Event(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, MQ.ROUTE_PIPELINE_RENAME)
data class PipelineRenameEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    val version: Int,
    override val userId: String,
    override var actionType: ActionType = ActionType.START,
    override var delayMills: Int = 0
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
