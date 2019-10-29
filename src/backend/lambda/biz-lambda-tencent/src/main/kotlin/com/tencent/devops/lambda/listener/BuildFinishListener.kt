package com.tencent.devops.lambda.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.lambda.service.PipelineBuildService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuildFinishListener @Autowired constructor(
    private val pipelineBuildService: PipelineBuildService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] Receive build finish event - ($event)")
        pipelineBuildService.onBuildFinish(event)
    }
}