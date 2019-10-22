package com.tencent.devops.common.event.pojo.pipeline

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType

/**
 * 构建结束的广播事件，用于通知等
 * @author irwinsun
 * @version 1.0
 */
@Event(MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, MQ.ROUTE_PIPELINE_BUILD_FINISH)
data class PipelineBuildFinishBroadCastEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val status: String,
    override var actionType: ActionType = ActionType.REFRESH,
    override var delayMills: Int = 0,
    val startTime: Long?,
    val endTime: Long?,
    val triggerType: String,
    var errorType: String? = null,
    val errorCode: Int? = null,
    var errorMsg: String? = null
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
