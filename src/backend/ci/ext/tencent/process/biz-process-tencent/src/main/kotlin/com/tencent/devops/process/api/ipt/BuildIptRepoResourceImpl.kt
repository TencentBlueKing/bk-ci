package com.tencent.devops.process.api.ipt

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.process.pojo.ipt.IptBuildCommitInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildIptRepoResourceImpl @Autowired constructor(): BuildIptRepoResource {
    override fun getCommitBuildCommitInfo(pipelineId: String, commitId: String): IptBuildCommitInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCommitBuildArtifactorytInfo(pipelineId: String, commitId: String): IptBuildArtifactoryInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}