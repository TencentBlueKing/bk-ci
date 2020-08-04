package com.tencent.devops.project.resources.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceAuthResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthResourceImpl @Autowired constructor(
    private val projectService: ProjectService
) : ServiceAuthResource {
    override fun list(limit: Int, offset: Int): Result<Page<ProjectVO>> {
        return Result(projectService.list(limit, offset))
    }
}