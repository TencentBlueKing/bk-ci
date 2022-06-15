package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent

/**
 * 流水线开启stream事件
 *
 * @version 1.0
 */
@Event(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, MQ.ROUTE_PIPELINE_STREAM_ENABLED)
data class PipelineStreamEnabledEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    override var actionType: ActionType = ActionType.START,
    override var delayMills: Int = 0
    // 之后添加上 repoId、repoUrl两个信息
    // 之后可能不会继承这个pipelineEvent了
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)