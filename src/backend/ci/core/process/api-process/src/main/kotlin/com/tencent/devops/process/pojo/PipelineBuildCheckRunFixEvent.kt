package com.tencent.devops.process.pojo

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import com.tencent.devops.common.stream.constants.StreamBinding

@Event(StreamBinding.PIPELINE_BUILD_CHECK_RUN_FIX)
data class PipelineBuildCheckRunFixEvent(
    override val projectId: String,
    override val pipelineId: String,
    val buildId: String,
    override var actionType: ActionType = ActionType.REFRESH,
    override val source: String,
    override val userId: String,
    override var delayMills: Int = 0,
    override var retryTime: Int = 3
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills, retryTime)