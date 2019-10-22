package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityControlPointResource
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.service.v2.QualityControlPointService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityControlPointMarketResourceImpl @Autowired constructor(
    private val controlPointService: QualityControlPointService
) : ServiceQualityControlPointResource {

    override fun set(userId: String, controlPoint: QualityControlPoint): Result<Int> {
        return Result(controlPointService.serviceCreateOrUpdate(userId, controlPoint))
    }

    override fun cleanTestProject(controlPointType: String): Result<Int> {
        return Result(controlPointService.cleanTestProject(controlPointType))
    }
}