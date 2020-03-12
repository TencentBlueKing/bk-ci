package com.tencent.devops.openapi.resources.v2


import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.v2.ApigwArtifactoryResourceV2
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryResourceV2Impl @Autowired constructor(
    private val client: Client
): ApigwArtifactoryResourceV2 {
    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?
    ): Result<List<String>> {
        return client.get(ServiceArtifactoryDownLoadResource::class).getThirdPartyDownloadUrl(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = artifactoryType,
            path = path,
            ttl = ttl,
            crossPipineId = crossPipineId,
            crossProjectId = crossProjectId,
            crossBuildNo = crossBuildNo
        )
    }
}