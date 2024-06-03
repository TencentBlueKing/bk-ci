package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import com.tencent.devops.process.service.builds.PipelineBuildCommitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineBuildCommitResourceImpl @Autowired constructor(
    private val buildCommitService: PipelineBuildCommitService
) : ServicePipelineBuildCommitResource {

    override fun save(commits: List<PipelineBuildCommit>): Result<Boolean> {
        buildCommitService.saveCommits(commits)
        return Result(true)
    }
}
