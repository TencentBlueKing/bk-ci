package com.tencent.devops.lambda.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildElementFinishBroadCastEvent
import com.tencent.devops.lambda.service.PipelineBuildService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuildElementFinishListener @Autowired constructor(
    private val pipelineBuildService: PipelineBuildService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildElementFinishBroadCastEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildElementFinishBroadCastEvent){
        logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] Receive build element finish event - ($event)")
        pipelineBuildService.onBuildElementFinish(event)
    }
}