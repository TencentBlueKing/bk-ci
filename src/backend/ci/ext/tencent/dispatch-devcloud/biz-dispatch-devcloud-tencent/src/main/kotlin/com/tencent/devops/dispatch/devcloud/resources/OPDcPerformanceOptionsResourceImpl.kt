package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.op.OPDcPerformanceOptionsResource
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceOptionsService

@RestResource
class OPDcPerformanceOptionsResourceImpl constructor(
    private val dcPerformanceOptionsService: DcPerformanceOptionsService
) : OPDcPerformanceOptionsResource {
    override fun listDcPerformanceOptions(userId: String): Result<List<PerformanceOptionsVO>> {
        return Result(dcPerformanceOptionsService.listDcPerformanceConfig(userId))
    }

    override fun createDcPerformanceOptions(
        userId: String,
        performanceOptionsVO: PerformanceOptionsVO
    ): Result<Boolean> {
        return Result(dcPerformanceOptionsService.createDcPerformanceOptions(userId, performanceOptionsVO))
    }

    override fun updateDcPerformanceOptions(
        userId: String,
        id: Long,
        performanceOptionsVO: PerformanceOptionsVO
    ): Result<Boolean> {
        return Result(dcPerformanceOptionsService.updateDcPerformanceOptions(userId, id, performanceOptionsVO))
    }

    override fun deleteDcPerformanceOptions(userId: String, id: Long): Result<Boolean> {
        return Result(dcPerformanceOptionsService.deleteDcPerformanceOptions(userId, id))
    }
}
