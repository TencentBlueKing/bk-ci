package com.tencent.devops.environment.config.async

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.MDC

@Event(AsyncExecute.ENV_ASYNC_EXECUTE)
data class AsyncExecuteEvent(
    val traceId: String? = MDC.get(TraceTag.BIZID),
    val eventStr: String,
    val type: AsyncExecuteEventType,
    override var delayMills: Int = 0,
    override var retryTime: Int = 1
) : IEvent(delayMills = delayMills, retryTime = retryTime)

enum class AsyncExecuteEventType {
    ASYNC_INSTALL_IMATE,
}
