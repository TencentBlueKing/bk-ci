package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.OPControlPointResource
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointData
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointUpdate
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.service.v2.QualityControlPointService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPControlPointResourceImpl @Autowired constructor(
    private val controlPointService: QualityControlPointService
) : OPControlPointResource {

    override fun list(userId: String, page: Int?, pageSize: Int?): Result<Page<ControlPointData>> {
        checkParams(userId)
        val result = controlPointService.opList(userId, page!!, pageSize!!)
        return Result(result)
    }

    override fun update(
        userId: String,
        id: Long,
        controlPointUpdate: ControlPointUpdate
    ): Result<Boolean> {
        checkParams(userId, id)
        val updateResult = controlPointService.opUpdate(userId, id, controlPointUpdate)
        return Result(updateResult)
    }

    override fun getStage(userId: String): Result<List<String>> {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        return Result(controlPointService.opGetStages())
    }

    override fun getElementName(userId: String): Result<List<ElementNameData>> {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        return Result(controlPointService.opGetElementNames())
    }

    private fun checkParams(userId: String, id: Long = 1) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        if (id <= 0L) throw ParamBlankException("Invalid id")
    }
}