package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityControlPointResource
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.response.ControlPointStageGroup
import com.tencent.devops.quality.service.v2.QualityControlPointService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityControlPointResourceImpl @Autowired constructor(
    val controlPointService: QualityControlPointService
) : UserQualityControlPointResource {
    override fun listElementType(userId: String, projectId: String): Result<List<String>> {
        return Result(controlPointService.userList(userId, projectId).filter { it.enable }.map { it.type })
    }

    override fun get(userId: String, elementType: String): Result<QualityControlPoint> {
        return Result(controlPointService.userGetByType(elementType) ?: throw OperationException("control point ($elementType) not found"))
    }

    override fun list(userId: String, projectId: String): Result<List<ControlPointStageGroup>> {
        return Result(controlPointService.userList(userId, projectId).filter { it.enable }.groupBy { it.stage }.map { ControlPointStageGroup(it.key, it.value) })
    }
}