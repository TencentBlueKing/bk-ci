package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.remotedev.RemoteDevMQ
import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.MDC

@Event(RemoteDevMQ.REMOTE_DEV_ASYNC_EXECUTE)
data class AsyncExecuteEvent(
    val traceId: String? = MDC.get(TraceTag.BIZID),
    val eventStr: String,
    val type: AsyncExecuteEventType
) : IEvent()

enum class AsyncExecuteEventType {
    ASYNC_PIPELINE,
    ASYNC_JOB_END,
    ASYNC_TGIT_ACL_IP,
    ASYNC_TGIT_ACL_USER,
    ASYNC_TCLOUD_CFS,
    ASYNC_JOB_PIPELINE,
    ASYNC_NOTIFY,
    ASYNC_USER_AUTH_CHECK
}
