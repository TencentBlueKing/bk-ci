package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.monitoring.api.service.SlaMonitorResource
import com.tencent.devops.monitoring.pojo.SlaCodeccResponseData
import com.tencent.devops.openapi.api.apigw.v2.ApigwSlaResourceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwSlaResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwSlaResourceV2 {
    override fun codeccQueryByBG(
        appCode: String?,
        apigwType: String?,
        userId: String,
        bgId: String,
        startTime: Long,
        endTime: Long
    ): Result<SlaCodeccResponseData> {
        logger.info("codeccQueryByBG , userId:$userId , bgId:$bgId , startTime:$startTime , endTime:$endTime")
        return client.get(SlaMonitorResource::class).codeccQuery(bgId, startTime, endTime)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwSlaResourceV2Impl::class.java)
    }
}