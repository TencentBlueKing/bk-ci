package com.tencent.devops.process.pojo.mq

import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.IEvent

abstract class IDispatchEvent(
    open var actionType: ActionType,
    open val source: String,
    open val projectId: String,
    open val pipelineId: String,
    open val userId: String,
    open val routeKeySuffix: String? = null,
    override var delayMills: Int,
    override var retryTime: Int = 1
) : IEvent(delayMills, retryTime)
