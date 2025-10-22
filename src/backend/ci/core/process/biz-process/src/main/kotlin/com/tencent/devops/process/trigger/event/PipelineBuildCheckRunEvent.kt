package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.stream.constants.StreamBinding

@Event(StreamBinding.PIPELINE_BUILD_CHECK_RUN)
data class PipelineBuildCheckRunEvent(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildStatus: BuildStatus,
    override var delayMills: Int = 0,
    override var retryTime: Int = 3
) : IEvent()
