package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.util.timestampmilli
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SamplePipelineBuildExtService @Autowired constructor(
    private val dslContext: DSLContext,
    private val fileDao: FileDao
) : PipelineBuildExtService {
    override fun getArtifactList(projectId: String, pipelineId: String, buildId: String): List<FileInfo> {
        val props = mapOf(
            "pipelineId" to pipelineId,
            "buildId" to buildId
        )
        val fileTypeList = listOf(FileTypeEnum.BK_ARCHIVE.fileType, FileTypeEnum.BK_CUSTOM.fileType)
        val fileInfoRecords = fileDao.getFileListByProps(dslContext, projectId, fileTypeList, props, null, null)
        val fileInfoList = mutableListOf<FileInfo>()
        fileInfoRecords?.forEach {
            var artifactoryType = ArtifactoryType.PIPELINE
            if (it["fileType"] == FileTypeEnum.BK_CUSTOM.fileType) {
                artifactoryType = ArtifactoryType.CUSTOM_DIR
            }
            fileInfoList.add(
                FileInfo(
                    name = it["fileName"] as String,
                    fullName = it["fileName"] as String,
                    path = it["filePath"] as String,
                    fullPath = it["filePath"] as String,
                    size = it["fileSize"] as Long,
                    folder = false,
                    modifiedTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    artifactoryType = artifactoryType
                )
            )
        }
        return fileInfoList
    }

    override fun synArtifactoryInfo(
        userId: String,
        artifactList: List<FileInfo>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNum: Int
    ) {
    }
}