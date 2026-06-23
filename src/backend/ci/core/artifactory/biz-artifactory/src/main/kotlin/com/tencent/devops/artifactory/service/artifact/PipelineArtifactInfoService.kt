/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.artifactory.service.artifact

import com.tencent.devops.artifactory.dao.PipelineArtifactInfoDao
import com.tencent.devops.artifactory.pojo.artifact.ArtifactMetadataRequest
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.model.artifactory.tables.records.TPipelineArtifactInfoRecord
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
    private val pipelineArtifactInfoDao: PipelineArtifactInfoDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineArtifactInfoService::class.java)
    }

    /**
     * 保存产出物元数据
     */
    fun saveArtifactInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        request: ArtifactMetadataRequest
    ): Long {
        logger.info(
            "Save artifact info: projectId=$projectId, pipelineId=$pipelineId, buildId=$buildId, " +
                "artifactType=${request.artifactType}, artifactName=${request.artifactName}, " +
                "artifactVersion=${request.artifactVersion}"
        )

        val id = pipelineArtifactInfoDao.create(
            dslContext = dslContext,
            artifactInfo = mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "pipelineName" to request.pipelineName,
                "buildId" to buildId,
                "buildNum" to request.buildNum,
                "stageId" to (request.stageId ?: ""),
                "containerId" to (request.containerId ?: ""),
                "taskId" to (request.taskId ?: ""),
                "executeCount" to (request.executeCount ?: 1),
                "artifactType" to request.artifactType,
                "artifactName" to request.artifactName,
                "artifactVersion" to (request.artifactVersion ?: ""),
                "artifactUri" to request.artifactUri,
                "artifactRepoUrl" to request.artifactRepoUrl,
                "artifactDigest" to request.artifactDigest,
                "artifactSize" to request.artifactSize,
                "codeRepoUrl" to request.codeRepoUrl,
                "commitId" to (request.commitId ?: ""),
                "extraInfo" to request.extraInfo,
                "creator" to userId,
                "modifier" to userId
            )
        )

        logger.info("Saved artifact info successfully, id=$id")
        return id
    }

    /**
     * 查询产出物元数据
     */
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

        return convertToPipelineArtifactInfo(record)
    }

    /**
     * 查询构建的产出物列表
     */
    fun listArtifactsByBuild(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<PipelineArtifactInfo> {
        val records = pipelineArtifactInfoDao.listByBuild(
            dslContext = dslContext,
            pipelineId = pipelineId,
            buildId = buildId
        )

        return records.mapNotNull { convertToPipelineArtifactInfo(it) }
    }

    /**
     * 清理产出物元数据（按构建 ID 列表）
     */
    fun cleanupByBuildIds(buildIds: List<String>): Int {
        if (buildIds.isEmpty()) {
            return 0
        }
        logger.info("Cleanup artifact info by buildIds, count=${buildIds.size}")
        return pipelineArtifactInfoDao.deleteByBuildIds(dslContext, buildIds)
    }

    /**
     * 清理产出物元数据（按项目和时间）
     */
    fun cleanupByProjectAndTime(projectId: String, beforeTime: java.time.LocalDateTime): Int {
        logger.info("Cleanup artifact info: projectId=$projectId, beforeTime=$beforeTime")
        return pipelineArtifactInfoDao.deleteByProjectAndTime(dslContext, projectId, beforeTime)
    }

    /**
     * 统计项目的产出物数量
     */
    fun countByProject(projectId: String): Long {
        return pipelineArtifactInfoDao.countByProject(dslContext, projectId)
    }

    /**
     * 将数据库记录转换为实体类
     */
    private fun convertToPipelineArtifactInfo(record: TPipelineArtifactInfoRecord): PipelineArtifactInfo? {
        return try {
            PipelineArtifactInfo(
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
        } catch (e: Exception) {
            logger.warn("Failed to convert record to PipelineArtifactInfo: ${e.message}")
            null
        }
    }
}
