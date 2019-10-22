package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityIndicatorResourceImpl @Autowired constructor(
    private val qualityIndicatorService: QualityIndicatorService
) : ServiceQualityIndicatorResource {

    override fun get(projectId: String, indicatorId: String): Result<QualityIndicator> {
        return Result(qualityIndicatorService.serviceGet(projectId, indicatorId))
    }

    override fun appendRangeByElement(elementType: String, projectIds: Collection<String>): Result<Int> {
        return Result(qualityIndicatorService.appendRangeByElement(elementType, projectIds))
    }
}