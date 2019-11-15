package com.tencent.devops.store.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.store.service.common.StoreBuildService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StorePipelineBuildFinishListener @Autowired constructor(
    private val storeBuildService: StoreBuildService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        logger.info("store event is:$event")
        val result = storeBuildService.handleStoreBuildStatus(
            event.userId,
            event.buildId,
            event.pipelineId,
            BuildStatus.valueOf(event.status)
        )
        logger.info("the handleStoreBuildStatus result is:$result")
    }
}
