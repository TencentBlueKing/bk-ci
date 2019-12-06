package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityControlPointMarketResource
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.service.v2.QualityControlPointService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityControlPointMarketResourceImpl @Autowired constructor(
    private val controlPointService: QualityControlPointService
) : ServiceQualityControlPointMarketResource {

    override fun setTestControlPoint(userId: String, controlPoint: QualityControlPoint): Result<Int> {
        return Result(controlPointService.setTestControlPoint(userId, controlPoint))
    }

    override fun refreshControlPoint(elementType: String): Result<Int> {
        return Result(controlPointService.refreshControlPoint(elementType))
    }

    override fun deleteTestControlPoint(elementType: String): Result<Int> {
        return Result(controlPointService.deleteTestControlPoint(elementType))
    }
}