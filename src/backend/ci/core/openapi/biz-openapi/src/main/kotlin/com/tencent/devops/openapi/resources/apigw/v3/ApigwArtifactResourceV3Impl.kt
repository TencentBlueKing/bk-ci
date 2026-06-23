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
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AuthResourceType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 产出物元数据查询 OpenAPI 实现
 * 通过 Client 远程调用 artifactory 模块的 ServiceArtifactMetadataResource
 */
@Service
@RestResource
@Suppress("ALL")
class ApigwArtifactResourceV3Impl @Autowired constructor(
    private val client: Client,
    private val clientTokenService: ClientTokenService
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
        artifactName: String,
        artifactVersion: String
    ): Result<PipelineArtifactInfo?> {
        logger.info(
            "OPENAPI_ARTIFACT_V3|$userId|getArtifactInfo|$projectId|$pipelineId|$artifactType|$artifactName|$artifactVersion"
        )

        // 1. 如果指定了 pipelineId，校验流水线查看权限
        val hasPermission = if (!pipelineId.isNullOrBlank()) {
            checkPipelineViewPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        } else {
            // 未指定 pipelineId，校验项目权限
            checkProjectPermission(
                userId = userId,
                projectId = projectId
            )
        }
        if (!hasPermission) {
            logger.warn("User $userId has no permission to access project $projectId")
            return Result(null)
        }

        // 2. 通过 Client 调用 artifactory 模块的 service 接口
        val artifactInfo = kotlin.runCatching {
            client.get(ServiceArtifactMetadataResource::class)
                .getArtifactInfo(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    artifactType = artifactType,
                    artifactName = artifactName,
                    artifactVersion = artifactVersion
                ).data
        }.getOrElse { e ->
            logger.warn("Failed to get artifact info: ${e.message}")
            null
        }

        logger.info(
            "OPENAPI_ARTIFACT_V3|$userId|getArtifactInfo|$projectId|$pipelineId|$artifactType|$artifactName|$artifactVersion|" +
                "found=${artifactInfo != null}"
        )

        return Result(artifactInfo)
    }

    /**
     * 校验流水线查看权限
     * 调用 auth 模块的 validateUserResourcePermissionByRelation 接口
     */
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
                    resourceType = AuthResourceType.PIPELINE.name,
                    action = AuthPermission.VIEW.name
                ).data ?: false
        } catch (e: Exception) {
            logger.warn("Failed to check pipeline view permission: ${e.message}")
            false
        }
    }

    /**
     * 校验项目权限
     * 调用 auth 模块的 isProjectUser 接口
     */
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
