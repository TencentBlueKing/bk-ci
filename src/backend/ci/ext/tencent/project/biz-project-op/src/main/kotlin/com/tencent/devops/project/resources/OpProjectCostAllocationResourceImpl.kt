package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpProjectCostAllocationResource
import com.tencent.devops.project.service.ProjectBillsService
import com.tencent.devops.project.service.ProjectOperationalProductService

@RestResource
class OpProjectCostAllocationResourceImpl constructor(
    val projectBillsService: ProjectBillsService,
    val projectOperationalProductService: ProjectOperationalProductService
) : OpProjectCostAllocationResource {
    override fun processInactiveProject(projectList: List<String>): Result<Boolean> {
        return Result(
            projectBillsService.checkInactiveProjectRegularly(
                projectList = projectList
            )
        )
    }

    override fun processInactiveProjectByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Result<Boolean> {
        return Result(
            projectBillsService.checkInactiveProjectRegularly(
                projectConditionDTO = projectConditionDTO
            )
        )
    }

    override fun syncOperationalProduct(): Result<Boolean> {
        return Result(projectOperationalProductService.syncOperationalProduct())
    }

    override fun reportBillsData(): Result<Boolean> {
        return Result(projectBillsService.reportBillsData())
    }
}
