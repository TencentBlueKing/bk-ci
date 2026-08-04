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

import com.tencent.devops.artifactory.api.service.ServiceArtifactMetadataResource
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfoQuery
import com.tencent.devops.artifactory.service.artifact.PipelineArtifactInfoService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceArtifactMetadataResourceImpl(
    private val pipelineArtifactInfoService: PipelineArtifactInfoService
) : ServiceArtifactMetadataResource {

    override fun listArtifactInfo(
        userId: String,
        projectId: String,
        pipelineId: String?,
        artifactType: String?,
        artifactName: String?,
        artifactVersion: String?,
        executeCount: Int?,
        buildId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineArtifactInfo>> {
        val artifactInfo = pipelineArtifactInfoService.listArtifactInfo(
            userId = userId,
            query = PipelineArtifactInfoQuery(
                projectId = projectId,
                artifactType = artifactType,
                artifactName = artifactName,
                artifactVersion = artifactVersion,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount
            ),
            page = page ?: 1,
            pageSize = pageSize ?: 20
        )
        return Result(artifactInfo)
    }
}
