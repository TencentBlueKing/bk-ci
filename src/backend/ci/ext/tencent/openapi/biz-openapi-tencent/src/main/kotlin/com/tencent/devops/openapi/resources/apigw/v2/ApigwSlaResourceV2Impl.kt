package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwSlaResourceV2
import com.tencent.devops.openapi.pojo.ErrorPie
import com.tencent.devops.openapi.pojo.SlaCodeccResponseData
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
        beginDate: Long?,
        endDate: Long?
    ): Result<SlaCodeccResponseData> {
        logger.info("codeccQueryByBG , userId:$userId , bgId:$bgId , beginDate:$beginDate , endDate:$endDate")

        // TODO 查询真实数据

        return Result(
            SlaCodeccResponseData(
                count = 10,
                costTime = 10000L,
                successRate = 0.9,
                errorPie = listOf(
                    ErrorPie(
                        code = "9200001",
                        message = "测试",
                        count = 1
                    )
                )
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwSlaResourceV2Impl::class.java)
    }
}