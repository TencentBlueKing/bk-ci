package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.UserArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.BadRequestException

@RestResource
class UserArtifactoryResourceImpl @Autowired constructor(
    val artifactoryService: ArtifactoryService,
    val artifactorySearchService: ArtifactorySearchService,
    val artifactoryDownloadService: ArtifactoryDownloadService
) : UserArtifactoryResource {
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

    override fun search(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        bkSearchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = artifactorySearchService.search(userId, projectId, bkSearchProps, offset, pageSizeNotNull)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun searchFileAndProperty(
        userId: String,
        projectId: String,
        bkSearchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        checkParameters(userId, projectId)
        val result = artifactorySearchService.searchFileAndProperty(userId, projectId, bkSearchProps)
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

    override fun folderSize(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FolderSize> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryService.folderSize(userId, projectId, artifactoryType, path))
    }

    override fun downloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryDownloadService.getDownloadUrl(userId, projectId, artifactoryType, path))
    }

    override fun ioaUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<Url> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryDownloadService.getIoaUrl(userId, projectId, artifactoryType, path))
    }

    override fun shareUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        downloadUsers: String
    ): Result<Boolean> {
        checkParameters(userId, projectId, path)
        if (ttl < 0) {
            throw InvalidParamException("Invalid ttl")
        }
        if (downloadUsers.isBlank()) {
            throw InvalidParamException("Invalid downloadUsers")
        }
        artifactoryDownloadService.shareUrl(userId, projectId, artifactoryType, path, ttl, downloadUsers)
        return Result(true)
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
        return Result(artifactoryDownloadService.getExternalUrl(userId, projectId, artifactoryType, path))
    }

    override fun getFilePipelineInfo(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FilePipelineInfo> {
        checkParameters(userId, projectId, path)
        return Result(artifactoryService.getFilePipelineInfo(userId, projectId, artifactoryType, path))
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