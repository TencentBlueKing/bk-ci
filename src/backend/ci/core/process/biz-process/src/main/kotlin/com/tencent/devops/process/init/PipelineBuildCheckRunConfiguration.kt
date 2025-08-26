package com.tencent.devops.process.init

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.trigger.check.PipelineCheckRunService
import com.tencent.devops.process.trigger.event.PipelineBuildCheckRunEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 流水线检查监听器配置
 */
@Configuration
class PipelineBuildCheckRunConfiguration {
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

    @EventConsumer
    fun checkRunSendConsumer(
        @Autowired pipelineCheckRunService: PipelineCheckRunService
    ) = ScsConsumerBuilder.build<PipelineBuildCheckRunEvent> {
        pipelineCheckRunService.onBuildCheckRun(it)
    }
}
