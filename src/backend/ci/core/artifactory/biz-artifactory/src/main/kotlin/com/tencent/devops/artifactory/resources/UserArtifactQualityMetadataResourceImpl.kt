/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.resources

import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelDetail
import com.tencent.devops.artifactory.api.user.UserArtifactQualityMetadataResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.pojo.MetadataLabelDetailUpdate
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import java.time.LocalDateTime

@RestResource
class UserArtifactQualityMetadataResourceImpl(
    private val bkRepoClient: BkRepoClient,
    private val client: Client
) : UserArtifactQualityMetadataResource {
    override fun list(
        userId: String,
        projectId: String
    ): Result<List<MetadataLabelDetail>> {
        return Result(
            bkRepoClient.listArtifactQualityMetadataLabels(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun listByPipeline(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<MetadataLabelDetail>> {
        // 1. 获取制品列表（优化空值处理）
        val artifactList = client.get(ServiceBuildResource::class)
            .getLatestBuildInfo(projectId, pipelineId, debug = false)
            .data?.artifactList.orEmpty()

        // 2. 提取元数据Key（优化去重逻辑）
        val propertyKeys = artifactList
            .flatMap { it.properties.orEmpty() }
            .mapTo(HashSet()) { it.key }

        // 3. 获取项目元数据标签（优化API调用）
        val labelMap = bkRepoClient.listArtifactQualityMetadataLabels(userId, projectId)
            .associateBy { it.labelKey }

        // 4. 构建结果集（优化查找逻辑）
        val result = propertyKeys.map { key ->
            labelMap[key] ?: MetadataLabelDetail(  // 使用Map直接查找
                labelKey = key,
                labelColorMap = emptyMap(),
                display = true,
                createdBy = "",
                enumType = false,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = "",
                lastModifiedDate = LocalDateTime.now(),
                category = "",
                system = false,
                enableColorConfig = false,
                description = ""
            )
        }.sortedBy { it.labelKey }

        return Result(result)
    }

    override fun get(
        userId: String,
        projectId: String,
        labelKey: String
    ): Result<MetadataLabelDetail> {
        return Result(
            bkRepoClient.getArtifactQualityMetadataLabel(
                userId = userId,
                projectId = projectId,
                labelKey = labelKey
            )
        )
    }

    override fun delete(
        userId: String,
        projectId: String,
        labelKey: String
    ): Result<Boolean> {
        bkRepoClient.deleteArtifactQualityMetadataLabel(
            userId = userId,
            projectId = projectId,
            labelKey = labelKey
        )
        return Result(true)
    }

    override fun update(
        userId: String,
        projectId: String,
        labelKey: String,
        metadataLabelUpdate: MetadataLabelDetailUpdate
    ): Result<Boolean> {
        bkRepoClient.updateArtifactQualityMetadataLabel(
            userId = userId,
            projectId = projectId,
            labelKey = labelKey,
            metadataLabelUpdate = metadataLabelUpdate
        )
        return Result(true)
    }

    override fun create(
        userId: String,
        projectId: String,
        metadataLabel: MetadataLabelDetail
    ): Result<Boolean> {
        bkRepoClient.createArtifactQualityMetadataLabel(
            userId = userId,
            projectId = projectId,
            metadataLabel = metadataLabel
        )
        return Result(true)
    }
}
