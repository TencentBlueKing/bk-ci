package com.tencent.devops.gitci.resources

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.CurrentBuildResource
import com.tencent.devops.gitci.pojo.GitCIModelDetail
import com.tencent.devops.gitci.service.CurrentBuildService
import com.tencent.devops.gitci.service.GitProjectConfService
import com.tencent.devops.process.pojo.Report
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class CurrentBuildResourceImpl @Autowired constructor(
    private val currentBuildService: CurrentBuildService,
    private val gitProjectConfService: GitProjectConfService
) : CurrentBuildResource {
    override fun getLatestBuildDetail(userId: String, gitProjectId: Long, buildId: String?): Result<GitCIModelDetail?> {
        checkParam(userId, gitProjectId)

        return if (buildId.isNullOrBlank()) {
            Result(currentBuildService.getLatestBuildDetail(userId, gitProjectId))
        } else {
            Result(currentBuildService.getBuildDetail(userId, gitProjectId, buildId!!))
        }
    }

    override fun search(
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        searchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(userId, gitProjectId)
        return Result(currentBuildService.search(userId, gitProjectId, page, pageSize, searchProps))
    }

    override fun downloadUrl(
        userId: String,
        gitProjectId: Long,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParam(userId, gitProjectId)
        return Result(currentBuildService.downloadUrl(userId, gitProjectId, artifactoryType, path))
    }

    override fun getReports(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): Result<List<Report>> {
        checkParam(userId, gitProjectId)

        return Result(currentBuildService.getReports(userId, gitProjectId, pipelineId, buildId))
    }


    private fun checkParam(userId: String, gitProjectId: Long) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }

        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
    }
}
