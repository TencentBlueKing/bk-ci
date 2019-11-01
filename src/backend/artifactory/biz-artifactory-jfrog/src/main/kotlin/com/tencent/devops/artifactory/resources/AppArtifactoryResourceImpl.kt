package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.AppArtifactoryResource
import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.AppArtifactoryService
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.BadRequestException

@RestResource
class AppArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val appArtifactoryService: AppArtifactoryService
) : AppArtifactoryResource {

    override fun list(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<FileInfo>> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryService.list(userId, projectId, artifactoryType, path))
    }

    override fun getOwnFileList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = artifactoryService.getOwnFileList(userId, projectId, limit.offset, limit.limit)
        return Result(FileInfoPage(0L, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun getBuildFileList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<AppFileInfo>> {
        checkParameters(userId, projectId)
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val result = artifactoryService.getBuildFileList(userId, projectId, pipelineId, buildId)
        return Result(result)
    }

    override fun search(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        SearchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = artifactorySearchService.search(userId, projectId, SearchProps, offset, pageSizeNotNull)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun searchFileAndProperty(
        userId: String,
        projectId: String,
        SearchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val result = artifactorySearchService.searchFileAndProperty(userId, projectId, SearchProps)
        return Result(FileInfoPage(result.second.size.toLong(), 0, 0, result.second, result.first))
    }

    override fun show(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryService.show(userId, projectId, artifactoryType, path))
    }

    override fun properties(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryService.getProperties(projectId, artifactoryType, path))
    }

    override fun externalUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val result =
            appArtifactoryService.getExternalDownloadUrl(userId, projectId, artifactoryType, path, 24 * 3600, false)
        return Result(result)
    }

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val result =
            appArtifactoryService.getExternalDownloadUrl(userId, projectId, artifactoryType, path, 24 * 3600, true)
        return Result(result)
    }

    private fun checkParameters(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkParameters(userId: String, projectId: String, path: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }
}