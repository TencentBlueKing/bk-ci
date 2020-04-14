package com.tencent.devops.common.pipeline.listener

import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.event.pojo.pipeline.PipelineHardDeleteBroadCastEvent
import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class PipelineHardDeleteMQListener : Listener<PipelineHardDeleteBroadCastEvent> {

    private val logger = LoggerFactory.getLogger(PipelineHardDeleteMQListener::class.java)

    override fun execute(event: PipelineHardDeleteBroadCastEvent) {
        try {
            onReceivePipelineHardDelete(event)
        } catch (ex: Exception) {
            logger.error("pipeline hard delete listener execute error", ex)
        }
    }

    private fun onReceivePipelineHardDelete(event: PipelineHardDeleteBroadCastEvent) {
        logger.info("receive event:{}", event)
        PipelineListenerUtil.getHardDeleteListeners().forEach { listener ->
            logger.info("invoke $listener")
            var deleteResult = false
            var retryCount = 0
            try {
                val dslContext = SpringContextUtil.getBean(DSLContext::class.java)
                while (!deleteResult && retryCount < 3) {
                    retryCount += 1
                    deleteResult = listener.onPipelineDeleteHardly(dslContext, event.pipelineBuildBaseInfoList.map { PipelineBuildBaseInfo(it.first, it.second, it.third) })
                }
            } catch (e: Exception) {
                logger.error("PipelineHardDelete Listener not implemented", e)
            }
        }
    }
}