package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.engine.service.PipelineNotifyService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class PipelineNotifyQueueListener(
    private val pipelineNotifyService: PipelineNotifyService,
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
            this.onPipelineShutdown(event)
        } finally {
            MDC.remove(TraceTag.BIZID)
        }
    }

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        this.execute(event)
    }

    private fun onPipelineShutdown(event: PipelineBuildFinishBroadCastEvent, retryCount: Int = 0) {
        if (retryCount < 3) {
            try {
                with(event) {
                    pipelineNotifyService.onPipelineShutdown(pipelineId = pipelineId,
                        buildId = buildId,
                        projectId = projectId,
                        buildStatus = BuildStatus.parse(status))
                }
            } catch (e: Exception) {
                LOG.warn("${event.buildId}|pipeline notify failed, retry count $retryCount", e)
                this.onPipelineShutdown(event, retryCount + 1)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineNotifyQueueListener::class.java)
    }
}
