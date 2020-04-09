package com.tencent.devops.repository.listener

import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.event.pojo.pipeline.PipelineHardDeleteBroadCastEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineHardDeleteMQListener @Autowired constructor(
) : Listener<PipelineHardDeleteBroadCastEvent> {

    private val logger = LoggerFactory.getLogger(PipelineHardDeleteMQListener::class.java)

    override fun execute(event: PipelineHardDeleteBroadCastEvent) {
        try {
            onReceivePipelineHardDelete(event)
        } catch (ex: Exception) {
            logger.error("project listener execute error", ex)
        }
    }

    fun onReceivePipelineHardDelete(event: PipelineHardDeleteBroadCastEvent) {
        logger.info("receive event:{}", event)
        //TODO
        event.pipelineBuildBaseInfoList.forEach {

        }
    }
}