package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.user.UserPipelineOutputResource
import com.tencent.devops.artifactory.pojo.PipelineOutput
import com.tencent.devops.artifactory.pojo.PipelineOutputSearchOption
import com.tencent.devops.artifactory.service.PipelineOutputService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class UserPipelineOutputResourceImpl(
    private val pipelineOutputService: PipelineOutputService
) : UserPipelineOutputResource {
    override fun searchByBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        option: PipelineOutputSearchOption?
    ): Result<List<PipelineOutput>> {
        return Result(pipelineOutputService.search(userId, projectId, pipelineId, buildId, option))
    }
}
