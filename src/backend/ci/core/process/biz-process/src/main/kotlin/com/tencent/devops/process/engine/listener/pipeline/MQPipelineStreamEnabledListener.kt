package com.tencent.devops.process.engine.listener.pipeline

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.process.engine.pojo.event.PipelineStreamEnabledEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线开启Stream事件
 *
 * @version 1.0
 */
@Component
class MQPipelineStreamEnabledListener @Autowired constructor(
    private val callBackControl: CallBackControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineStreamEnabledEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineStreamEnabledEvent) {
        val watcher = Watcher(id = "${event.traceId}|StreamEnabled#${event.pipelineId}|${event.userId}")
        try {
            watcher.start("callback")
            callBackControl.pipelineStreamEnabledEvent(
                event = event
            )
            watcher.stop()
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }
}
