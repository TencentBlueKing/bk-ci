package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.OpEnvResource
import com.tencent.devops.environment.pojo.ProjectConfig
import com.tencent.devops.environment.pojo.ProjectConfigPage
import com.tencent.devops.environment.pojo.ProjectConfigParam
import com.tencent.devops.environment.service.BcsClusterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpEnvResourceImpl @Autowired constructor(
    private val bcsClusterService: BcsClusterService
) : OpEnvResource {

    override fun saveProjectConfig(projectConfigParam: ProjectConfigParam): Result<Boolean> {
        bcsClusterService.saveProjectConfig(projectConfigParam)
        return Result(true)
    }

    override fun listProjectConfig(): Result<List<ProjectConfig>> {
        return Result(bcsClusterService.listProjectConfig())
    }

    override fun list(page: Int, pageSize: Int, projectId: String?): Result<ProjectConfigPage> {
        return Result(ProjectConfigPage(bcsClusterService.countProjectConfig(projectId), bcsClusterService.list(page, pageSize, projectId)))
    }
}