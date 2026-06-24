/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.artifactory.api.service.ServiceArtifactMetadataResource
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwArtifactResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@RestResource
class ApigwArtifactResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactResourceV3 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactResourceV3Impl::class.java)
    }

    override fun getArtifactInfo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String?,
        artifactVersion: String?
    ): Result<PipelineArtifactInfo?> {
        logger.info(
            "OPENAPI_ARTIFACT_V3|$userId|getArtifactInfo|$projectId|$pipelineId|$artifactType|$artifactName|$artifactVersion"
        )

        val artifactInfo = client.get(ServiceArtifactMetadataResource::class)
            .getArtifactInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                artifactType = artifactType,
                artifactName = artifactName,
                artifactVersion = artifactVersion
            ).data

        logger.info(
            "OPENAPI_ARTIFACT_V3|$userId|getArtifactInfo|$projectId|$pipelineId|$artifactType|$artifactName|$artifactVersion|" +
                    "found=${artifactInfo != null}"
        )

        return Result(artifactInfo)
    }
}
