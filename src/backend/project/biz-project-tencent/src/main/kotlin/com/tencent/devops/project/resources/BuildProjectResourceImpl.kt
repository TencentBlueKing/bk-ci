package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.BuildProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildProjectResourceImpl @Autowired constructor(private val projectService: ProjectService) :
        BuildProjectResource {
    override fun listByProjectCode(projectCode: String): Result<List<ProjectVO>> {
        return Result(projectService.list(setOf(projectCode)))
    }
}