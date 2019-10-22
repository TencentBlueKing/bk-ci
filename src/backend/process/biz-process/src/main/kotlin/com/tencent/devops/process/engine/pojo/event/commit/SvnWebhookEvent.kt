package com.tencent.devops.process.engine.pojo.event.commit

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.process.engine.pojo.event.commit.enum.CommitEventType


@Event(MQ.EXCHANGE_SVN_BUILD_REQUEST_EVENT, MQ.ROUTE_SVN_BUILD_REQUEST_EVENT)
data class SvnWebhookEvent(
    override var requestContent: String,
    override var retryTime: Int = 3,
    override var delayMills: Int = 0,
    override val commitEventType: CommitEventType = CommitEventType.SVN
) : ICodeWebhookEvent(requestContent, retryTime, delayMills, commitEventType)