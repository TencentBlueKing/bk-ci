package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpProjectBillResource
import com.tencent.devops.project.service.ProjectBillsService
import com.tencent.devops.project.service.ProjectOperationalProductService

@RestResource
class OpProjectBillResourceImpl constructor(
    val projectBillsService: ProjectBillsService,
    val projectOperationalProductService: ProjectOperationalProductService
) : OpProjectBillResource {
    override fun processInactiveProject(projectList: List<String>): Result<Boolean> {
        return Result(
            projectBillsService.checkInactiveProject(
                projectList = projectList
            )
        )
    }

    override fun processInactiveProjectByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Result<Boolean> {
        return Result(
            projectBillsService.checkInactiveProject(
                projectConditionDTO = projectConditionDTO
            )
        )
    }

    override fun checkProjectRelatedProduct(projectList: List<String>): Result<Boolean> {
        return Result(
            projectBillsService.checkProjectRelatedProduct(
                projectList = projectList
            )
        )
    }

    override fun syncOperationalProduct(): Result<Boolean> {
        return Result(projectOperationalProductService.syncOperationalProduct())
    }

    override fun reportBillsData(yearAndMonthOfReportStr: String): Result<Boolean> {
        return Result(projectBillsService.reportBillsData(yearAndMonthOfReportStr))
    }
}
