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
import com.tencent.bkrepo.repository.pojo.metadata.label.UserLabelCreateRequest
import com.tencent.bkrepo.repository.pojo.metadata.label.UserLabelUpdateRequest
import com.tencent.devops.artifactory.api.user.UserArtifactQualityMetadataResource
import com.tencent.devops.artifactory.pojo.MetadataLabelSimpleInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.BkRepoClient
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

    override fun listSimple(
        userId: String,
        projectId: String
    ): Result<List<MetadataLabelSimpleInfo>> {
        val result = bkRepoClient.listArtifactQualityMetadataLabels(
            userId = userId,
            projectId = projectId
        ).map {
            MetadataLabelSimpleInfo(
                key = it.labelKey,
                values = it.labelColorMap.keys.toList()
            )
        }
        return Result(result)
    }

    /**
     * 根据流水线ID获取其最近一次构建产物所关联的元数据标签列表。
     * 1. 获取流水线最近构建的产物。
     * 2. 提取所有产物中不重复的元数据Key。
     * 3. 获取项目下已定义的所有元数据标签。
     * 4. 合并以上两组数据：
     *    - 如果产物中的Key在项目标签中已定义，则使用项目标签的完整信息。
     *    - 如果未定义，则为其创建一个临时的、默认的标签信息。
     * 5. 按Key排序后返回。
     */
    override fun listByPipeline(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<MetadataLabelDetail>> {
        // 获取项目下所有已定义的元数据标签，并转换为Map以便高效查找
        val definedLabels = bkRepoClient.listArtifactQualityMetadataLabels(userId, projectId)
            .associateBy { it.labelKey }

        // 获取流水线产物中的所有元数据Key
        val artifactPropertyKeys = client.get(ServiceBuildResource::class)
            .getLatestBuildInfo(projectId, pipelineId, debug = false)
            .data?.artifactList.orEmpty()
            .flatMap { it.properties.orEmpty() }
            .mapTo(HashSet()) { it.key } // 使用HashSet自动去重

        // 将产物中的Key与已定义的标签进行匹配和构建
        val resultLabels = artifactPropertyKeys.map { key ->
            definedLabels[key] ?: MetadataLabelDetail(
                labelKey = key,
                labelColorMap = emptyMap(),
                display = true, // 默认显示
                createdBy = "custom",
                enumType = false,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = "custom",
                lastModifiedDate = LocalDateTime.now(),
                category = "Uncategorized",
                system = false,
                description = "Auto-generated for an artifact property not defined in project.",
                enableColorConfig = false
            )
        }.sortedBy { it.labelKey }

        return Result(resultLabels)
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
        metadataLabelUpdate: UserLabelUpdateRequest
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
        metadataLabel: UserLabelCreateRequest
    ): Result<Boolean> {
        bkRepoClient.createArtifactQualityMetadataLabel(
            userId = userId,
            projectId = projectId,
            metadataLabel = metadataLabel
        )
        return Result(true)
    }
}
