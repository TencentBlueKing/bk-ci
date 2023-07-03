package com.tencent.devops.remotedev.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.remotedev.MQ.EXCHANGE_WORKSPACE_UPDATE_FROM_K8S
import com.tencent.devops.common.remotedev.MQ.ROUTE_WORKSPACE_REMINDER
import com.tencent.devops.common.remotedev.WorkspaceEvent
import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.MDC

@Event(EXCHANGE_WORKSPACE_UPDATE_FROM_K8S, ROUTE_WORKSPACE_REMINDER)
data class RemoteDevReminderEvent(
    override val userId: String,
    override val traceId: String = MDC.get(TraceTag.BIZID),
    override val workspaceName: String,
    override val delayMills: Int = 0,
    override val retryTime: Int = 0
) : WorkspaceEvent(userId, traceId, workspaceName, delayMills, retryTime)
