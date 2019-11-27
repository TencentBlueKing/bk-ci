package com.tencent.devops.process.api.ipt

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.process.service.ipt.IptRepoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildIptRepoResourceImpl @Autowired constructor(
    private val iptRepoService: IptRepoService
) : BuildIptRepoResource {

    override fun getCommitBuildArtifactorytInfo(
        projectId: String,
        pipelineId: String,
        userId: String,
        commitId: String
    ): Result<IptBuildArtifactoryInfo> {
        return Result(iptRepoService.getCommitBuildArtifactorytInfo(projectId, pipelineId, userId, commitId))
    }

}