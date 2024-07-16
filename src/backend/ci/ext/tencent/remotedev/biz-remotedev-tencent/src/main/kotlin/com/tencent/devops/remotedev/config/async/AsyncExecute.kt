package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.async.AsyncExecuteEventData
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge

object AsyncExecute {
    fun dispatch(streamBridge: StreamBridge, data: AsyncExecuteEventData) {
        dispatch(
            streamBridge, AsyncExecuteEvent(
                eventStr = JsonUtil.toJson(data, false),
                type = data.toType()
            )
        )
    }

    private fun dispatch(streamBridge: StreamBridge, event: AsyncExecuteEvent) {
        try {
            logger.info("AsyncExecuteDispatch|${event.type}|${event.eventStr}")
            event.sendTo(streamBridge)
        } catch (e: Throwable) {
            logger.error("AsyncExecuteDispatch|error:", e)
        }
    }

    private val logger = LoggerFactory.getLogger(AsyncExecute::class.java)
}
