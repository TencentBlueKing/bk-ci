package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.trigger.event.CdsWebhookRequestEvent
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketEventResourceImpl @Autowired constructor(
    private val simpleDispatcher: SampleEventDispatcher
) : ServiceMarketEventResource {
    override fun cdsWebhook(
        userId: String,
        projectId: String,
        workspaceName: String,
        cdsIp: String,
        eventType: String,
        eventCode: String,
        body: String
    ): Result<Boolean> {
        simpleDispatcher.dispatch(
            CdsWebhookRequestEvent(
                userId = userId,
                projectId = projectId,
                workspaceName = workspaceName,
                cdsIp = cdsIp,
                eventCode = eventCode,
                body = body,
                eventType = eventType
            )
        )
        return Result(true)
    }
}
