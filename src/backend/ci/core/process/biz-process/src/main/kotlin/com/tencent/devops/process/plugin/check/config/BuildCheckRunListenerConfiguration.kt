package com.tencent.devops.process.plugin.check.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.plugin.check.service.PipelineCheckRunService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 流水线检查监听器配置
 */
@Configuration
@Suppress("TooManyFunctions")
class BuildCheckRunListenerConfiguration {
    @EventConsumer(groupName = "pipeline-check-run-finish")
    fun checkRunFinishConsumer(
        @Autowired pipelineCheckRunService: PipelineCheckRunService
    ) = ScsConsumerBuilder.build<PipelineBuildFinishBroadCastEvent> {
        pipelineCheckRunService.onBuildFinished(it)
    }

    @EventConsumer(groupName = "pipeline-check-run-queue")
    fun checkRunQueueConsumer(
        @Autowired pipelineCheckRunService: PipelineCheckRunService
    ) = ScsConsumerBuilder.build<PipelineBuildQueueBroadCastEvent> {
        pipelineCheckRunService.onBuildQueue(it)
    }
}
