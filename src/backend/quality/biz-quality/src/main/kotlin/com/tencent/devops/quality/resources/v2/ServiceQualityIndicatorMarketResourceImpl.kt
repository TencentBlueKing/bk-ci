package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorMarketResource
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityIndicatorMarketResourceImpl @Autowired constructor(
    private val qualityIndicatorService: QualityIndicatorService
) : ServiceQualityIndicatorMarketResource {
    override fun setTestIndicator(userId: String, atomCode: String, indicatorUpdateList: Collection<IndicatorUpdate>): Result<Int> {
        return Result(qualityIndicatorService.setTestIndicator(userId, atomCode, indicatorUpdateList))
    }

    override fun refreshIndicator(elementType: String, metadataMap: Map<String, String>): Result<Int> {
        return Result(qualityIndicatorService.serviceRefreshIndicator(elementType, metadataMap))
    }

    override fun deleteTestIndicator(elementType: String): Result<Int> {
        return Result(qualityIndicatorService.serviceDeleteTestIndicator(elementType))
    }
}