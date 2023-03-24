package com.tencent.devops.process.pojo.mq

import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.pipeline.type.DispatchType

abstract class IDispatchEvent(
    open var actionType: ActionType,
    open val source: String,
    open val projectId: String,
    open val pipelineId: String,
    open val userId: String,
    open val dispatchType: DispatchType?,
    override var delayMills: Int,
    override var retryTime: Int = 1
) : IEvent(delayMills, retryTime)
