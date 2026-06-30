package com.tencent.devops.environment.config.async

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.AsyncExecuteEventData
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge

object AsyncExecute {
    fun dispatch(
        streamBridge: StreamBridge,
        data: AsyncExecuteEventData,
        errorLogTag: String? = null,
        delayMills: Int = 0
    ) {
        dispatch(
            streamBridge = streamBridge,
            event = AsyncExecuteEvent(
                eventStr = JsonUtil.toJson(data, false),
                type = data.toType(),
                delayMills = delayMills
            ),
            errorLogTag = errorLogTag
        )
    }

    fun dispatch(streamBridge: StreamBridge, event: AsyncExecuteEvent, errorLogTag: String? = null) {
        try {
            logger.info("AsyncExecuteDispatch|${event.type}|${event.eventStr}")
            event.sendTo(streamBridge)
        } catch (e: Throwable) {
            if (errorLogTag.isNullOrBlank()) {
                logger.error("AsyncExecuteDispatch|error:", e)
            } else {
                // 针对某些特殊的需要配置告警的场景添加
                logger.error("$errorLogTag|AsyncExecuteDispatch|error:", e)
            }
        }
    }

    private val logger = LoggerFactory.getLogger(AsyncExecute::class.java)

    const val ENV_ASYNC_EXECUTE = "environment.async.execute"
}
