package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.api.user.UserArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwArtifactoryResourceV2
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryResourceV2 {
    override fun getThirdPartyDownloadUrl(
        appCode: String?,
        apigwType: String?,
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

    override fun getUserDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        return client.get(UserArtifactoryResource::class).downloadUrl(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        )
    }

    override fun search(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        return client.get(UserArtifactoryResource::class).search(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            searchProps = searchProps
        )
    }
}