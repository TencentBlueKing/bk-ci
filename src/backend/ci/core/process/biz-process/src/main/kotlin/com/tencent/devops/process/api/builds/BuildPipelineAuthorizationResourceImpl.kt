package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPipelineAuthorizationResourceImpl @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService
) : BuildPipelineAuthorizationResource {
    override fun getPipelineAuthorization(projectId: String, pipelineId: String): Result<String?> {
        return Result(pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId))
    }
}
