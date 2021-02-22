package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineWebHookQueueListener @Autowired constructor(
    private val pipelineWebHookQueueService: PipelineWebHookQueueService
) {

    fun onBuildStart(event: PipelineBuildStartBroadCastEvent) {
        pipelineWebHookQueueService.onBuildStart(event)
    }

    fun onBuildFinish(event: PipelineBuildFinishBroadCastEvent) {
        pipelineWebHookQueueService.onBuildFinish(event)
    }
}
