package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceArtifactoryBluekingResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import javax.ws.rs.BadRequestException

@RestResource
class ServiceArtifactoryBluekingResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val artifactoryDownloadService: ArtifactoryDownloadService
) : ServiceArtifactoryBluekingResource {

    @Value("\${jfrog.docker_url}")
    private val jfrogDockerUrl: String = ""
    @Value("\${jfrog.docker_port}")
    private val jfrogDockerPort: String = ""

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
        val result = artifactoryService.acrossProjectCopy(
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path,
            targetProjectId = targetProjectId,
            targetPath = targetPath
        )
        return Result(result)
    }

    override fun properties(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<List<Property>> {
        checkParam(projectId)
        val result = artifactoryService.getProperties(
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path
        )
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
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path,
            ttl = ttl,
            directed = isDirected
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
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            argPath = path,
            ttl = ttl,
            directed = isDirected
        )
        return Result(result)
    }

    override fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<FileDetail> {
        checkParam(projectId)
        return Result(artifactoryService.show(projectId = projectId, artifactoryType = artifactoryType, path = path))
    }

//    override fun search(projectId: String, page: Int?, pageSize: Int?, searchProps: List<Property>): Result<FileInfoPage<FileInfo>> {
//        checkParam(projectId)
//        val pageNotNull = page ?: 0
//        val pageSizeNotNull = pageSize ?: -1
//        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
//        val result = artifactorySearchService.serviceSearch(projectId, searchProps, offset, pageSizeNotNull)
//        return Result(FileInfoPage(0, pageNotNull, pageSizeNotNull, result.second, result.first))
//    }

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
            artifactorySearchService.serviceSearchFileByRegex(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                regexPath = regexPath,
                customized = customized
            )
        return Result(FileInfoPage(
            count = 0,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            records = result.second,
            timestamp = result.first
        ))
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
        return Result(FileInfoPage(
            count = 0,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            records = result.second,
            timestamp = result.first
        ))
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
        return Result(FileInfoPage(
            count = 0,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            records = result.second,
            timestamp = result.first
        ))
    }

    override fun createDockerUser(projectId: String): Result<DockerUser> {
        checkParam(projectId)
        val result = artifactoryService.createDockerUser(projectId)
        return Result(DockerUser(
            user = result.user,
            password = result.password,
            domain = jfrogDockerUrl,
            docker_port = jfrogDockerPort
        ))
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
        artifactoryService.setDockerProperties(
            projectId = projectId,
            imageName = imageName,
            tag = tag,
            properties = properties
        )
        return Result(true)
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}