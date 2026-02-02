package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwMarketEventResourceV4
import com.tencent.devops.process.api.service.ServiceMarketEventResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwMarketEventResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwMarketEventResourceV4 {
    override fun cdsWebhook(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        workspaceName: String,
        cdsIp: String,
        eventType: String,
        eventCode: String,
        body: String
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_MARKET_EVENT_V4|$userId|remote dev webhook|$projectId||$eventCode|$eventType|" +
                    "$workspaceName|$cdsIp"
        )
        return client.get(ServiceMarketEventResource::class).cdsWebhook(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            cdsIp = cdsIp,
            eventType = eventType,
            eventCode = eventCode,
            body = body
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwMarketEventResourceV4Impl::class.java)
    }
}
