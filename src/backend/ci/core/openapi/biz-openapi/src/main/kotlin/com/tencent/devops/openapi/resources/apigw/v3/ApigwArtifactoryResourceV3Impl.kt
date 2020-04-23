package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.artifactory.api.UserPipelineFileResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwArtifactoryResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryResourceV3 {
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryResourceV3Impl::class.java)
    }

    override fun getUserDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        return client.get(UserPipelineFileResource::class).downloadUrl(
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
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<FileInfo>> {
        val map = mutableMapOf<String, String>()
        map["pipelineId"] = pipelineId
        map["buildId"] = buildId
        val searchProps = SearchProps(
            fileNames = null,
            props = map
        )
        return client.get(UserPipelineFileResource::class).searchFile(
            userId = userId,
            projectCode = projectId,
            page = page,
            pageSize = pageSize,
            searchProps = searchProps
        )
    }
}