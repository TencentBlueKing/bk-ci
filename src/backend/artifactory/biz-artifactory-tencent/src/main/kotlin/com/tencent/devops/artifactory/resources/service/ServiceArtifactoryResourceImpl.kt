package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.ArtifactoryCreateInfo
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.ArtifactoryInfoService
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.BadRequestException

@RestResource
class ServiceArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val artifactoryInfoService: ArtifactoryInfoService
) : ServiceArtifactoryResource {

    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<Boolean> {
        checkParam(projectId)
        val result = artifactoryService.check(projectId, artifactoryType, path)
        return Result(result)
    }

    override fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        checkParam(projectId)
        val result = artifactoryService.acrossProjectCopy(projectId, artifactoryType, path, targetProjectId, targetPath)
        return Result(result)
    }

    override fun properties(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<List<Property>> {
        checkParam(projectId)
        val result = artifactoryService.getProperties(projectId, artifactoryType, path)
        return Result(result)
    }

    override fun externalUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val isDirected = directed ?: false
        val result = artifactoryDownloadService.serviceGetExternalDownloadUrl(
            userId,
            projectId,
            artifactoryType,
            path,
            ttl,
            isDirected
        )
        return Result(result)
    }

    override fun downloadUrl(
        projectId: String,
        artifactoryType: ArtifactoryType,
        userId: String,
        path: String,
        ttl: Int,
        directed: Boolean?
    ): Result<Url> {
        checkParam(projectId)
        if (!path.endsWith(".ipa") && !path.endsWith(".apk")) {
            throw BadRequestException("Path must end with ipa or apk")
        }
        val isDirected = directed ?: false
        val result = artifactoryDownloadService.serviceGetInnerDownloadUrl(
            userId,
            projectId,
            artifactoryType,
            path,
            ttl,
            isDirected
        )
        return Result(result)
    }

    override fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<FileDetail> {
        checkParam(projectId)
        return Result(artifactoryService.show(projectId, artifactoryType, path))
    }

    override fun search(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = artifactorySearchService.serviceSearch(projectId, searchProps, offset, pageSizeNotNull)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun searchFile(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean,
        page: Int?,
        pageSize: Int?
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val result =
            artifactorySearchService.serviceSearchFileByRegex(projectId, pipelineId, buildId, regexPath, customized)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun searchFileAndPropertyByAnd(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val result = artifactorySearchService.serviceSearchFileAndProperty(projectId, searchProps)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun searchFileAndPropertyByOr(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>> {
        checkParam(projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val result = artifactorySearchService.serviceSearchFileAndPropertyByOr(projectId, searchProps)
        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
    }

    override fun createDockerUser(projectId: String): Result<DockerUser> {
        checkParam(projectId)
        val result = artifactoryService.createDockerUser(projectId)
        return Result(DockerUser(result.user, result.password))
    }

    override fun setProperties(
        projectId: String,
        imageName: String,
        tag: String,
        properties: Map<String, String>
    ): Result<Boolean> {
        if (imageName.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        if (tag.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        artifactoryService.setDockerProperties(projectId, imageName, tag, properties)
        return Result(true)
    }

    override fun searchCustomFiles(projectId: String, condition: CustomFileSearchCondition): Result<List<String>> {
        checkParam(projectId)
        return Result(artifactoryService.listCustomFiles(projectId, condition))
    }

    override fun getJforgInfoByteewTime(
        startTime: Long,
        endTime: Long,
        page: Int,
        pageSize: Int
    ): Result<List<FileInfo>> {
        return Result(artifactorySearchService.getJforgInfoByteewTime(page, pageSize, startTime, endTime))
    }

    override fun createArtifactoryInfo(
        buildId: String,
        pipelineId: String,
        projectId: String,
        buildNum: Int,
        fileInfo: FileInfo,
        dataFrom: Int
    ): Result<Long> {
        checkInfoParam(buildId, pipelineId, fileInfo)
        val id = artifactoryInfoService.createInfo(buildId, pipelineId, projectId, buildNum, fileInfo, dataFrom)
        return Result(id)
    }

    override fun batchCreateArtifactoryInfo(infoList: List<ArtifactoryCreateInfo>): Result<Int> {
        return Result(artifactoryInfoService.batchCreateArtifactoryInfo(infoList))
    }

    private fun checkInfoParam(buildId: String, pipelineId: String, fileInfo: FileInfo) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}