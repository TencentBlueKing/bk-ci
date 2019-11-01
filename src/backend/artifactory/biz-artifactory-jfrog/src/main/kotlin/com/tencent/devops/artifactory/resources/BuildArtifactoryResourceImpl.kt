package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class BuildArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val jfrogService: JfrogService
) : BuildArtifactoryResource {

    @Value("\${jfrog.docker_url}")
    private var jfrogDockerUrl: String = ""
    @Value("\${jfrog.docker_port}")
    private var jfrogDockerPort: String = ""

    override fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        properties: Map<String, String>
    ): Result<Boolean> {
        checkParam(projectId, path)
        artifactoryService.setProperties(projectId, artifactoryType, path, properties)
        return Result(true)
    }

    override fun getProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<Property>> {
        checkParam(projectId, path)
        val result = artifactoryService.getProperties(projectId, artifactoryType, path)
        return Result(result)
    }

    override fun getPropertiesByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<FileDetail>> {
        return Result(artifactoryService.getPropertiesByRegex(projectId, pipelineId, buildId, artifactoryType, path))
    }

    override fun getThirdPartyDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int?
    ): Result<List<String>> {
        checkParam(projectId, path)
        return Result(
            artifactoryDownloadService.getThirdPartyDownloadUrl(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                artifactoryType = artifactoryType,
                argPath = path,
                ttl = ttl
            )
        )
    }

    override fun getFileDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<List<String>> {
        val param = ArtifactorySearchParam(
            projectId,
            pipelineId,
            buildId,
            path,
            artifactoryType == ArtifactoryType.CUSTOM_DIR,
            1
        )
        return Result(jfrogService.getFileDownloadUrl(param))
    }

    private fun checkParam(projectId: String, path: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }

    override fun createDockerUser(projectId: String): DockerUser {
        checkParam(projectId)
        val result = artifactoryService.createDockerUser(projectId)
        return (DockerUser(
            user = result.user,
            password = result.password,
            domain = jfrogDockerUrl,
            docker_port = jfrogDockerPort
        ))
    }

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}