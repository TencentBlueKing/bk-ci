package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineWebHookQueueListener @Autowired constructor(
    private val pipelineWebHookQueueService: PipelineWebHookQueueService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildStartBroadCastEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildStartBroadCastEvent) {
        pipelineWebHookQueueService.onBuildStart(event)
    }
}