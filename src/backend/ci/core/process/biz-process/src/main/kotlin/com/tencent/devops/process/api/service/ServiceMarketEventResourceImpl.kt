package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.trigger.GenericEventStartRequest
import com.tencent.devops.process.trigger.event.CdsWebhookRequestEvent
import com.tencent.devops.process.trigger.market.MarketEventTriggerBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketEventResourceImpl @Autowired constructor(
    private val simpleDispatcher: SampleEventDispatcher,
    private val marketEventTriggerBuildService: MarketEventTriggerBuildService
) : ServiceMarketEventResource {
    override fun cdsWebhook(
        userId: String,
        projectId: String,
        workspaceName: String,
        cdsIp: String,
        eventType: String,
        eventCode: String,
        body: Map<String, String>?
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

    override fun start(
        userId: String,
        projectId: String,
        pipelineId: String,
        eventCode: String,
        request: GenericEventStartRequest
    ): Result<BuildId> {
        return Result(
            marketEventTriggerBuildService.genericEventTrigger(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                eventCode = eventCode,
                request = request
            )
        )
    }
}
