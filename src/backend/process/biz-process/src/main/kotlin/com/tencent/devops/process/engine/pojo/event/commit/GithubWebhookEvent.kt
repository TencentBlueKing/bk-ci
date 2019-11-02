package com.tencent.devops.process.engine.pojo.event.commit

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.process.pojo.code.github.GithubWebhook

@Event(MQ.EXCHANGE_GITHUB_BUILD_REQUEST_EVENT, MQ.ROUTE_GITHUB_BUILD_REQUEST_EVENT)
data class GithubWebhookEvent(
    val githubWebhook: GithubWebhook,
    var retryTime: Int = 3,
    var delayMills: Int = 0
)