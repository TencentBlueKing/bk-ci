package com.tencent.devops.artifactory.service.artifact

import com.tencent.devops.artifactory.dao.PipelineArtifactInfoDao
import com.tencent.devops.artifactory.pojo.artifact.ArtifactMetadataRequest
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfoQuery
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
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
    private val clientTokenService: ClientTokenService,
    private val pipelineArtifactInfoDao: PipelineArtifactInfoDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineArtifactInfoService::class.java)
        private const val PIPELINE_ARTIFACT_INFO_BIZ_ID = "T_PIPELINE_ARTIFACT_INFO"
        private const val MAX_PAGE_SIZE = 1000
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

    fun listArtifactInfo(
        userId: String,
        query: PipelineArtifactInfoQuery,
        page: Int = 1,
        pageSize: Int = 20
    ): Page<PipelineArtifactInfo> {
        // 权限校验：指定 pipelineId → 校验流水线 VIEW 权限；否则 → 校验项目用户身份
        val hasPermission = if (!query.pipelineId.isNullOrBlank()) {
            checkPipelineViewPermission(userId, query.projectId, query.pipelineId!!)
        } else {
            checkProjectPermission(userId, query.projectId)
        }
        if (!hasPermission) {
            logger.warn(
                "Service artifact metadata: $userId has no permission to access " +
                        "${query.projectId}/${query.pipelineId}"
            )
            throw PermissionForbiddenException(
                message = "User $userId has no permission to access artifact metadata " +
                        "in project ${query.projectId}"
            )
        }

        val validPage = page.coerceAtLeast(1)
        val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val records = pipelineArtifactInfoDao.searchArtifactInfo(
            dslContext = dslContext,
            query = query,
            page = validPage,
            pageSize = validPageSize
        )
        // 首页且不满页时总数即本页条数，跳过 COUNT 查询
        val count = if (validPage == 1 && records.size < validPageSize) {
            records.size.toLong()
        } else {
            pipelineArtifactInfoDao.countArtifactInfo(dslContext, query)
        }
        return Page(validPage, validPageSize, count, records.map { convertToRecord(it) })
    }

    private fun checkPipelineViewPermission(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Boolean {
        return try {
            val token = clientTokenService.getSystemToken()
            client.get(ServicePermissionAuthResource::class)
                .validateUserResourcePermissionByRelation(
                    userId = userId,
                    token = token,
                    projectCode = projectId,
                    resourceCode = pipelineId,
                    resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                    action = "${AuthResourceType.PIPELINE_DEFAULT.value}_${AuthPermission.VIEW.value}",
                    relationResourceType = null
                ).data ?: false
        } catch (e: Exception) {
            logger.warn("Failed to check pipeline view permission: ${e.message}")
            false
        }
    }

    private fun checkProjectPermission(
        userId: String,
        projectId: String
    ): Boolean {
        return try {
            val token = clientTokenService.getSystemToken()
            client.get(ServiceProjectAuthResource::class)
                .isProjectUser(
                    token = token,
                    type = null,
                    userId = userId,
                    projectCode = projectId
                ).data ?: false
        } catch (e: Exception) {
            logger.warn("Failed to check project permission: ${e.message}")
            false
        }
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
