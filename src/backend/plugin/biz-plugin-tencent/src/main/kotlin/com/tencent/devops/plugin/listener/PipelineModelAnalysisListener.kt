package com.tencent.devops.plugin.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.plugin.service.CodeccElementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineModelAnalysisListener @Autowired constructor(
    private val codeccElementService: CodeccElementService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineModelAnalysisEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineModelAnalysisEvent) {
        try {
            codeccElementService.saveEvent(event)
        } catch (ex: Exception) {
            logger.error("Failed process received Pipeline Model Analysis message", ex)
            logger.error("error Pipeline Model Analysis data", event)
        }
    }
}
