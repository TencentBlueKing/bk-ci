package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.event.GenericWebhookRequestEvent
import com.tencent.devops.process.trigger.event.RemoteDevWebhookRequestEvent
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalEventResourceImpl @Autowired constructor(
    private val simpleDispatcher: SampleEventDispatcher
)  : ExternalEventResource {
    override fun remoteDevWebhook(
        userId: String,
        projectId: String,
        workspaceName: String,
        cdsIp: String,
        eventType: String,
        eventCode: String,
        body: String
    ): Result<Boolean> {
        simpleDispatcher.dispatch(
            RemoteDevWebhookRequestEvent(
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

    override fun genericWebhook(
        request: HttpServletRequest,
        eventCode: String,
        body: String
    ): Result<Boolean> {
        // 请求头
        val headers = request.headerNames.toList().associateWith { request.getHeader(it) ?: "" }
        // 查询参数
        val queryParams = request.parameterMap.mapValues { it.value.firstOrNull() ?: "" }
        simpleDispatcher.dispatch(
            GenericWebhookRequestEvent(
                eventCode = eventCode,
                request = WebhookRequest(
                    headers = headers,
                    queryParams = queryParams,
                    body = body
                ),
            )
        )
        return Result(true)
    }
}
