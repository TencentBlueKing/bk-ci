package com.tencent.devops.common.event.pojo.pipeline

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType

/**
 * 构建结束的广播事件，用于通知等
 */
@Event(MQ.EXCHANGE_PIPELINE_BUILD_ELEMENT_FINISH_FANOUT, MQ.ROUTE_PIPELINE_BUILD_ELEMENT_FINISH)
data class PipelineBuildElementFinishBroadCastEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val elementId: String,
    override var actionType: ActionType = ActionType.REFRESH,
    override var delayMills: Int = 0,
    val errorType: String? = null,
    val errorCode: Int? = null,
    val errorMsg: String? = null
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
