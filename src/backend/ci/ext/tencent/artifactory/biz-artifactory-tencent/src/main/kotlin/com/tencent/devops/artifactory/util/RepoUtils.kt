package com.tencent.devops.artifactory.util

import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RepoUtils {
    const val PIPELINE_REPO = "pipeline"
    const val CUSTOM_REPO = "custom"
    const val REPORT_REPO = "report"

    fun getRepoByType(repoType: ArtifactoryType): String {
        return when (repoType) {
            ArtifactoryType.PIPELINE -> PIPELINE_REPO
            ArtifactoryType.CUSTOM_DIR -> CUSTOM_REPO
        }
    }

    fun getTypeByRepo(repo: String): ArtifactoryType {
        return when (repo) {
            PIPELINE_REPO -> ArtifactoryType.PIPELINE
            CUSTOM_REPO -> ArtifactoryType.CUSTOM_DIR
            else -> throw IllegalArgumentException("invalid repo: $repo")
        }
    }

    fun isPipelineFile(fileInfo: com.tencent.bkrepo.generic.pojo.FileInfo): Boolean {
        return fileInfo.repoName == PIPELINE_REPO
    }

    fun toFileInfo(fileInfo: com.tencent.bkrepo.generic.pojo.FileInfo): FileInfo {
        val fullPath = refineFullPath(fileInfo)
        return FileInfo(
            name = fileInfo.name,
            fullName = fullPath,
            path = fileInfo.path,
            fullPath = fullPath,
            size = if (fileInfo.folder) -1 else fileInfo.size,
            folder = fileInfo.folder,
            modifiedTime = LocalDateTime.parse(fileInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            artifactoryType = getTypeByRepo(fileInfo.repoName)
        )
    }

    fun toFileDetail(fileDetail: com.tencent.bkrepo.generic.pojo.FileDetail): FileDetail {
        return FileDetail(
            name = fileDetail.fileInfo.name,
            path = fileDetail.fileInfo.path,
            fullName = fileDetail.fileInfo.fullPath,
            fullPath = fileDetail.fileInfo.fullPath,
            size = fileDetail.fileInfo.size,
            createdTime = LocalDateTime.parse(fileDetail.fileInfo.createdDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            modifiedTime = LocalDateTime.parse(fileDetail.fileInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            checksums = FileChecksums(fileDetail.fileInfo.sha256, "", ""),
            meta = fileDetail.metadata // todo  元数据补充 pipelineName
        )
    }

    private fun refineFullPath(fileInfo: com.tencent.bkrepo.generic.pojo.FileInfo): String {
        return if (fileInfo.folder && !fileInfo.fullPath.endsWith("/")) {
            fileInfo.fullPath + "/"
        } else {
            fileInfo.fullPath
        }
    }
}