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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryResourceV2 {
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryResourceV2Impl::class.java)
    }

    override fun getThirdPartyDownloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?
    ): Result<List<String>> {
        var pipelineId = ""
        var buildId = ""
        var subPath = path
        if (artifactoryType == ArtifactoryType.PIPELINE) {
            val pathList = path.split("/")
            logger.info("getThirdPartyDownloadUrl pathList:$pathList")
            if (pathList[0].isBlank()) {
                pipelineId = pathList[1]
                buildId = pathList[2]
            } else {
                pipelineId = pathList[0]
                buildId = pathList[1]
            }
            subPath = path.replace("/$pipelineId/$buildId" , "")
        }

        logger.info("getThirdPartyDownloadUrl pipelineId:$pipelineId")
        logger.info("getThirdPartyDownloadUrl buildId:$buildId")
        logger.info("getThirdPartyDownloadUrl subPath:$subPath")
        return client.get(ServiceArtifactoryDownLoadResource::class).getThirdPartyDownloadUrl(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryType = artifactoryType,
            path = subPath,
            ttl = ttl,
            crossPipineId = null,
            crossProjectId = null,
            crossBuildNo = null
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
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        val map = mutableMapOf<String, String>()
        map["pipelineId"] = pipelineId
        map["buildId"] = buildId
        val searchProps = SearchProps(
            fileNames = null,
            props = map
        )
        return client.get(UserArtifactoryResource::class).search(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            searchProps = searchProps
        )
    }
}