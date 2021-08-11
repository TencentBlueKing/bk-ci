package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.engine.service.PipelineSubscriptionService
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class PipelineNotifyQueueListener(
    private val pipelineSubscriptionService: PipelineSubscriptionService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    override fun execute(event: PipelineBuildFinishBroadCastEvent) {
        try {
            val traceId = MDC.get(TraceTag.BIZID)
            if (traceId.isNullOrEmpty()) {
                if (!event.traceId.isNullOrEmpty()) {
                    MDC.put(TraceTag.BIZID, event.traceId)
                } else {
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                }
            }
            with(event) {
                pipelineSubscriptionService.onPipelineShutdown(pipelineId = pipelineId,
                    buildId = buildId,
                    projectId = projectId,
                    buildStatus = BuildStatus.parse(status))
            }
        } finally {
            MDC.remove(TraceTag.BIZID)
        }
    }

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        this.execute(event)
    }
}
