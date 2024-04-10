package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpProjectCostAllocationResource
import com.tencent.devops.project.service.ProjectCostAllocationService

@RestResource
class OpProjectCostAllocationResourceImpl constructor(
    val projectCostAllocationService: ProjectCostAllocationService
) : OpProjectCostAllocationResource {
    override fun processInactiveProject(projectList: List<String>): Result<Boolean> {
        return Result(
            projectCostAllocationService.processInactiveProject(
                projectList = projectList
            )
        )
    }

    override fun processInactiveProjectByCondition(
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Result<Boolean> {
        return Result(
            projectCostAllocationService.processInactiveProjectByCondition(
                migrateProjectConditionDTO = migrateProjectConditionDTO
            )
        )
    }
}
