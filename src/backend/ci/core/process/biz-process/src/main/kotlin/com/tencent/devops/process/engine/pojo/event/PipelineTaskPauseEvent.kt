package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent

@Event(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, MQ.ROUTE_PIPELINE_PAUSE_TASK_EXECUTE)
class PipelineTaskPauseEvent(
    override var actionType: ActionType,
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    override var delayMills: Int = 0,
    val buildId: String,
    val taskId: String,
    val containerId: String,
    val stageId: String
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
