package com.tencent.devops.process.bkjob

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent

@Event(MQ.EXCHANGE_BKJOB_CLEAR_JOB_TMP_FANOUT, MQ.ROUTE_BKJOB_CLEAR_JOB_TMP_EVENT)
data class ClearJobTempFileEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val taskId: String,
    val clearFileSet: Set<String>,
    override var actionType: ActionType = ActionType.START,
    override var delayMills: Int = 0,
    override var routeKeySuffix: String?
) : IPipelineRoutableEvent(routeKeySuffix, actionType, source, projectId, pipelineId, userId, delayMills)
