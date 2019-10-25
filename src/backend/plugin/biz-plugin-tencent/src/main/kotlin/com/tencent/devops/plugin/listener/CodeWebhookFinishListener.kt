package com.tencent.devops.plugin.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.plugin.service.git.CodeWebhookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeWebhookFinishListener @Autowired constructor(
    private val codeWebhookService: CodeWebhookService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildFinishBroadCastEvent) {
        codeWebhookService.onFinish(event)
    }
}
