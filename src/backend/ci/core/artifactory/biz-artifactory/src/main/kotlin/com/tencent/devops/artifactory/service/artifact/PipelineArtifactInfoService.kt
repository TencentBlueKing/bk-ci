package com.tencent.devops.artifactory.service.artifact

import com.tencent.devops.artifactory.dao.PipelineArtifactInfoDao
import com.tencent.devops.artifactory.pojo.artifact.ArtifactMetadataRequest
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.artifactory.tables.records.TPipelineArtifactInfoRecord
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 流水线产出物元数据服务
 */
@Service
@Suppress("ALL")
class PipelineArtifactInfoService(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineArtifactInfoDao: PipelineArtifactInfoDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineArtifactInfoService::class.java)
        private const val PIPELINE_ARTIFACT_INFO_BIZ_ID = "T_PIPELINE_ARTIFACT_INFO"
    }

    fun saveArtifactInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        request: ArtifactMetadataRequest
    ) {
        logger.info(
            "Save artifact info: projectId=$projectId, pipelineId=$pipelineId, buildId=$buildId, " +
                    "artifactType=${request.artifactType}, artifactName=${request.artifactName}, " +
                    "artifactVersion=${request.artifactVersion}"
        )

        val id =
            client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_ARTIFACT_INFO_BIZ_ID).data
                ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.SYSTEM_ERROR,
                    params = arrayOf("Failed to generate segment ID for $PIPELINE_ARTIFACT_INFO_BIZ_ID")
                )
        val now = LocalDateTime.now()
        pipelineArtifactInfoDao.create(
            dslContext = dslContext,
            artifactInfo = PipelineArtifactInfo(
                id = id,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = request.pipelineName,
                buildId = buildId,
                buildNum = request.buildNum,
                stageId = request.stageId,
                containerId = request.containerId,
                taskId = request.taskId,
                executeCount = request.executeCount,
                artifactType = request.artifactType,
                artifactName = request.artifactName,
                artifactVersion = request.artifactVersion ?: "",
                artifactUri = request.artifactUri,
                artifactRepoUrl = request.artifactRepoUrl,
                artifactDigest = request.artifactDigest,
                artifactSize = request.artifactSize,
                codeRepoUrl = request.codeRepoUrl,
                commitId = request.commitId,
                extraInfo = request.extraInfo,
                creator = userId,
                modifier = userId,
                createTime = now,
                updateTime = now
            )
        )

        logger.info("Saved artifact info successfully")
    }

    fun getArtifactInfo(
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String,
        artifactVersion: String
    ): PipelineArtifactInfo? {
        val record = pipelineArtifactInfoDao.getByArtifact(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            artifactType = artifactType,
            artifactName = artifactName,
            artifactVersion = artifactVersion
        ) ?: return null

        return convertToRecord(record)
    }

    fun listArtifactsByBuild(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<PipelineArtifactInfo> {
        val records = pipelineArtifactInfoDao.listByBuild(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
        return records.map { convertToRecord(it) }
    }

    private fun convertToRecord(record: TPipelineArtifactInfoRecord): PipelineArtifactInfo {
        return PipelineArtifactInfo(
            id = record.id,
            projectId = record.projectId,
            pipelineId = record.pipelineId,
            pipelineName = record.pipelineName,
            buildId = record.buildId,
            buildNum = record.buildNum,
            stageId = record.stageId,
            containerId = record.containerId,
            taskId = record.taskId,
            executeCount = record.executeCount,
            artifactType = record.artifactType,
            artifactName = record.artifactName,
            artifactVersion = record.artifactVersion,
            artifactUri = record.artifactUri,
            artifactRepoUrl = record.artifactRepoUrl,
            artifactDigest = record.artifactDigest,
            artifactSize = record.artifactSize,
            codeRepoUrl = record.codeRepoUrl,
            commitId = record.commitId,
            extraInfo = record.extraInfo,
            creator = record.creator,
            modifier = record.modifier,
            createTime = record.createTime,
            updateTime = record.updateTime
        )
    }
}
