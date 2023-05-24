package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.MigrateProjectInfo
import com.tencent.devops.project.service.ProjectExtService
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.op.OpExtProjectResource
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO

@RestResource
class OpExtProjectResourceImpl @Autowired constructor(
    private val projectExtService: ProjectExtService
) : OpExtProjectResource {
    override fun getMigrateProjectInfo(): Result<List<MigrateProjectInfo>> {
        return Result(projectExtService.getMigrateProjectInfo())
    }

    override fun updateProjectCreator(
        projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>
    ):
        Result<Boolean> {
        return Result(
            projectExtService.updateProjectCreator(
                projectUpdateCreatorDtoList
                = projectUpdateCreatorDtoList
            )
        )
    }
}
