package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceArtifactoryDownLoadResourceImpl @Autowired constructor(
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : ServiceArtifactoryDownLoadResource {

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
        checkParam(projectId, path)
        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(
                bkRepoDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    artifactoryType,
                    path,
                    ttl,
                    crossProjectId,
                    crossPipineId,
                    crossBuildNo
                )
            )
        } else {
            Result(
                artifactoryDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    artifactoryType,
                    path,
                    ttl,
                    crossProjectId,
                    crossPipineId,
                    crossBuildNo
                )
            )
        }
    }

    private fun checkParam(projectId: String, path: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }
}