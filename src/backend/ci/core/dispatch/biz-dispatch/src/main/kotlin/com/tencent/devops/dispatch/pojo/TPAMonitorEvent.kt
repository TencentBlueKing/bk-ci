package com.tencent.devops.dispatch.pojo

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding.DISPATCH_AGENT_MONITOR

@Event(DISPATCH_AGENT_MONITOR)
data class TPAMonitorEvent(
    // 消息队列延迟消息时间
    override var delayMills: Int,
    // 超时时间，目前取job的执行超时时间
    val timeoutMin: Long,
    val projectId: String,
    val pipelineId: String,
    val userId: String,
    val buildId: String,
    val vmSeqId: String,
    val containerHashId: String?,
    val executeCount: Int?,
    val stepId: String?
) : IEvent() {
    fun toLog() = "TPAMonitorEvent|$timeoutMin|$projectId|$pipelineId|$userId|$buildId|$vmSeqId|$executeCount"
}
