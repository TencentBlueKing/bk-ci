package com.tencent.devops.project.resources.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceAuthProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService
) : ServiceAuthProjectResource {
    override fun list(limit: Int, offset: Int): Result<Page<ProjectVO>> {
        return Result(projectService.list(limit, offset))
    }

    override fun getByIds(ids: Set<String>): Result<List<ProjectVO>> {
        return Result(projectService.list(ids))
    }
}