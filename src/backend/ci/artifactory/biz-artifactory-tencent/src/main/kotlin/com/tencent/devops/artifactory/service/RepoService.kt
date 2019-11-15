package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.CopyToCustomReq
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import org.springframework.stereotype.Service

@Service
interface RepoService {

    fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo>

    fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail

    fun folderSize(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): FolderSize

    fun setDockerProperties(projectId: String, imageName: String, tag: String, properties: Map<String, String>)

    fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        properties: Map<String, String>
    )

    fun getProperties(projectId: String, artifactoryType: ArtifactoryType, argPath: String): List<Property>

    fun getPropertiesByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        argPath: String
    ): List<FileDetail>

    fun getOwnFileList(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<FileInfo>>

    fun getBuildFileList(userId: String, projectId: String, pipelineId: String, buildId: String): List<AppFileInfo>

    fun getFilePipelineInfo(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): FilePipelineInfo

    fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail

    fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Boolean

    fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count

    fun createDockerUser(projectCode: String): DockerUser

    fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String>

    fun copyToCustom(userId: String, projectId: String, pipelineId: String, buildId: String, copyToCustomReq: CopyToCustomReq)
}