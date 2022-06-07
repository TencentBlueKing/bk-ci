package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.process.engine.pojo.event.PipelineRenameEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("ALL")
@Component
class MQPipelineRenameListener @Autowired constructor(
    private val callBackControl: CallBackControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineRenameEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineRenameEvent) {
        val watcher = Watcher(id = "${event.traceId}|RenamePipeline#${event.pipelineId}|${event.userId}")
        try {
            watcher.start("callback")
            callBackControl.pipelineCreateEvent(projectId = event.projectId, pipelineId = event.pipelineId)
            watcher.stop()
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }
}