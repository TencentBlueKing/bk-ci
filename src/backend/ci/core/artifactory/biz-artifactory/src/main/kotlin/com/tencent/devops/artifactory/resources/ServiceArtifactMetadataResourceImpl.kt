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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.artifactory.service.artifact.PipelineArtifactInfoService

@RestResource
class ServiceArtifactMetadataResourceImpl(
    private val pipelineArtifactInfoService: PipelineArtifactInfoService
) : ServiceArtifactMetadataResource {
    override fun getArtifactInfo(
        userId: String,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String?,
        artifactVersion: String?
    ): Result<PipelineArtifactInfo?> {
        val artifactInfo = pipelineArtifactInfoService.getArtifactInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            artifactType = artifactType,
            artifactName = artifactName,
            artifactVersion = artifactVersion
        )
        return Result(artifactInfo)
    }
}
