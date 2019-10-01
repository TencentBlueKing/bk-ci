package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServcieProjectResourceImpl @Autowired constructor(
        private val projectService: ProjectService
        ):ServiceProjectResource {
    override fun getAllProject(): Result<List<ProjectVO>> {
        return Result(projectService.getAllProject())
    }
}