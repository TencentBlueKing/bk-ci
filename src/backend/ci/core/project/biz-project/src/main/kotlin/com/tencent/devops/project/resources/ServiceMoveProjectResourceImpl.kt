package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceMoveProjectResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMoveProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService
) : ServiceMoveProjectResource {
    override fun relationIamProject(projectCode: String, relationId: String): Result<Boolean> {
        return Result(projectService.relationIamProject(projectCode, relationId))
    }
}
