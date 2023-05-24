package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpMigrateProjectResource
import com.tencent.devops.project.pojo.MigrateProjectInfo
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.service.ProjectMigrateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMigrateProjectResourceImpl @Autowired constructor(
    private val projectMigrateService: ProjectMigrateService
) : OpMigrateProjectResource {
    override fun getMigrateProjectInfo(): Result<List<MigrateProjectInfo>> {
        return Result(projectMigrateService.getMigrateProjectInfo())
    }

    override fun updateProjectCreator(
        projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>
    ): Result<Boolean> {
        return Result(
            projectMigrateService.updateProjectCreator(
                projectUpdateCreatorDtoList = projectUpdateCreatorDtoList
            )
        )
    }
}
