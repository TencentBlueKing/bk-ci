/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactResource
import com.tencent.devops.artifactory.pojo.artifact.ArtifactMetadataRequest
import com.tencent.devops.artifactory.service.artifact.PipelineArtifactInfoService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 构建产出物元数据上报 API 实现
 */
@Service
@RestResource
@Suppress("ALL")
class BuildArtifactResourceImpl @Autowired constructor(
    private val pipelineArtifactInfoService: PipelineArtifactInfoService
) : BuildArtifactResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildArtifactResourceImpl::class.java)
    }

    override fun reportArtifactMetadata(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        request: ArtifactMetadataRequest
    ): Result<Long> {
        logger.info(
            "Report artifact metadata: userId=$userId, projectId=$projectId, pipelineId=$pipelineId, " +
                "buildId=$buildId, artifactType=${request.artifactType}, artifactName=${request.artifactName}"
        )

        val id = pipelineArtifactInfoService.saveArtifactInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            request = request
        )

        logger.info("Report artifact metadata success, id=$id")
        return Result(id)
    }
}
