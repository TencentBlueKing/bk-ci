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
import com.tencent.devops.artifactory.service.artifact.PipelineArtifactInfoService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory

@RestResource
class ServiceArtifactMetadataResourceImpl(
    private val client: Client,
    private val clientTokenService: ClientTokenService,
    private val pipelineArtifactInfoService: PipelineArtifactInfoService
) : ServiceArtifactMetadataResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceArtifactMetadataResourceImpl::class.java)
    }

    override fun getArtifactInfo(
        userId: String,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String?,
        artifactVersion: String?
    ): Result<PipelineArtifactInfo?> {
        // 权限校验：指定 pipelineId → 校验流水线 VIEW 权限；否则 → 校验项目用户身份
        val hasPermission = if (!pipelineId.isNullOrBlank()) {
            checkPipelineViewPermission(userId, projectId, pipelineId)
        } else {
            checkProjectPermission(userId, projectId)
        }
        if (!hasPermission) {
            logger.warn("Service artifact metadata: $userId has no permission to access $projectId/$pipelineId")
            return Result(null)
        }

        val artifactInfo = pipelineArtifactInfoService.getArtifactInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            artifactType = artifactType,
            artifactName = artifactName,
            artifactVersion = artifactVersion
        )
        return Result(artifactInfo)
    }

    private fun checkPipelineViewPermission(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Boolean {
        return try {
            val token = clientTokenService.getSystemToken() ?: ""
            client.get(ServicePermissionAuthResource::class)
                .validateUserResourcePermissionByRelation(
                    userId = userId,
                    token = token,
                    projectCode = projectId,
                    resourceCode = pipelineId,
                    resourceType = AuthResourceType.PIPELINE_DEFAULT.name,
                    action = AuthPermission.VIEW.name
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
            val token = clientTokenService.getSystemToken() ?: ""
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
}
