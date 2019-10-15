package com.tencent.devops.artifactory.resources.builds

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
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

@RestResource
class BuildArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val jfrogService: JfrogService
) : BuildArtifactoryResource {

    override fun getOwnFileList(userId: String, projectId: String): Result<FileInfoPage<FileInfo>> {
        val result = artifactoryService.getOwnFileList(userId, projectId, 0, -1)
        return Result(FileInfoPage(0L, 1, -1, result.second, result.first))
    }
    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Result<Boolean> {
        checkParam(projectId, path)
        val result = artifactoryService.check(projectId, artifactoryType, path)
        return Result(result)
    }

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

    override fun getPropertiesByRegex(projectId: String, pipelineId: String, buildId: String, artifactoryType: ArtifactoryType, path: String): Result<List<FileDetail>> {
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
        return Result(artifactoryDownloadService.getThirdPartyDownloadUrl(projectId, pipelineId, buildId, artifactoryType, path, ttl))
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

    private fun checkParam(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}