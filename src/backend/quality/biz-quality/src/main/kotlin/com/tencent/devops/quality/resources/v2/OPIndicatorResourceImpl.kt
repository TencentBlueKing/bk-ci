package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.OPIndicatorResource
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorData
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPIndicatorResourceImpl @Autowired constructor(
    private val indicatorService: QualityIndicatorService
) : OPIndicatorResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<Page<IndicatorData>> {
        checkParams(userId)
        val result = indicatorService.opList(userId, page, pageSize)
        return Result(result)
    }

    override fun getByIds(userId: String, ids: String): Result<List<IndicatorData>> {
        checkParams(userId)
        return Result(indicatorService.opListByIds(userId, ids))
    }

    override fun add(userId: String, indicatorUpdate: IndicatorUpdate): Result<Boolean> {
        checkParams(userId)
        if (indicatorUpdate.enName.isNullOrBlank()) {
            return Result(-1, "指标英文名不能为空", false)
        }
        val result = indicatorService.opCreate(userId, indicatorUpdate)
        return Result(result.code, result.msg, result.flag)
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        checkParams(userId, id)
        val result = indicatorService.opDelete(userId, id)
        return Result(result)
    }

    override fun update(userId: String, id: Long, indicatorUpdate: IndicatorUpdate): Result<Boolean> {
        checkParams(userId, id)
        val result = indicatorService.opUpdate(userId, id, indicatorUpdate)
        return Result(result.code, result.msg, result.flag)
    }

    private fun checkParams(userId: String, id: Long = 1) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        if (id <= 0L) throw ParamBlankException("Invalid id")
    }
}