package com.tencent.devops.plugin.codecc.event.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.event.PipelineSettingChangeEvent
import com.tencent.devops.plugin.codecc.service.PipelineCodeccService
import org.springframework.stereotype.Component

/**
 * 因为流水线的设置发生改变而需要通知处理Codecc
 */
@Component
class ChangeCodeCCListener(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineCodeccService: PipelineCodeccService
) : BaseListener<PipelineSettingChangeEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineSettingChangeEvent) {
        with(event) {
            pipelineCodeccService.updateCodeccTask(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName
            )
        }
    }
}