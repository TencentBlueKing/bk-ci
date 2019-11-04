package com.tencent.devops.process.engine.pojo.event.commit

import com.tencent.devops.process.engine.pojo.event.commit.enum.CommitEventType

abstract class ICodeWebhookEvent(
    open val requestContent: String,
    open val retryTime: Int,
    open val delayMills: Int,
    open val commitEventType: CommitEventType
)