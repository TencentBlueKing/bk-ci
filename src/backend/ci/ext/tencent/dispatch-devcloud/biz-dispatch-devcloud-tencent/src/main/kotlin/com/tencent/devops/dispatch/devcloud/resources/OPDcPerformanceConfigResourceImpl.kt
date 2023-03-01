package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.op.OPDcPerformanceConfigResource
import com.tencent.devops.dispatch.devcloud.pojo.performance.ListPage
import com.tencent.devops.dispatch.devcloud.pojo.performance.OPPerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceConfigService

@RestResource
class OPDcPerformanceConfigResourceImpl constructor(
    private val dcPerformanceConfigService: DcPerformanceConfigService
) : OPDcPerformanceConfigResource {
    override fun listDcPerformanceConfig(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<ListPage<PerformanceConfigVO>> {
        return Result(dcPerformanceConfigService.listDcPerformanceConfig(userId, page, pageSize))
    }

    override fun createDcPerformanceConfig(userId: String, opPerformanceConfigVO: OPPerformanceConfigVO): Result<Boolean> {
        return Result(dcPerformanceConfigService.createDcPerformanceConfig(userId, opPerformanceConfigVO))
    }

    override fun updateDcPerformanceConfig(
        userId: String,
        projectId: String,
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Result<Boolean> {
        return Result(dcPerformanceConfigService.updateDcPerformanceConfig(userId, projectId, opPerformanceConfigVO))
    }

    override fun deleteDcPerformanceConfig(userId: String, projectId: String): Result<Boolean> {
        return Result(dcPerformanceConfigService.deleteDcPerformanceConfig(userId, projectId))
    }
}
