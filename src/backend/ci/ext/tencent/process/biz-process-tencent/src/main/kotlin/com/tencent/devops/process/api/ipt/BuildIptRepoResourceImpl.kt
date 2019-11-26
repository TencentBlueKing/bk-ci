package com.tencent.devops.process.api.ipt

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.process.pojo.ipt.IptBuildCommitInfo
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildIptRepoResourceImpl @Autowired constructor(
        private val client: Client
): BuildIptRepoResource {
    override fun getCommitBuildCommitInfo(projectId: String, pipelineId: String, commitId: String): IptBuildCommitInfo {
        val buildId = client.get(ServiceRepositoryResource::class).getBuildIdByCommit(pipelineId, commitId).data
                ?: return IptBuildCommitInfo()
        return IptBuildCommitInfo(buildId, "")
    }

    override fun getCommitBuildArtifactorytInfo(projectId: String, pipelineId: String, commitId: String): IptBuildArtifactoryInfo {
        val buildId = client.get(ServiceRepositoryResource::class).getBuildIdByCommit(pipelineId, commitId).data
                ?: return IptBuildArtifactoryInfo()

        val searchProperty = listOf(Property("buildId", buildId), Property("pipelineId", pipelineId))
        val fileList = client.get(ServiceArtifactoryResource::class)
                .search(projectId, null, null, searchProperty).data?.records ?: listOf()
        return IptBuildArtifactoryInfo(buildId, fileList)
    }
}