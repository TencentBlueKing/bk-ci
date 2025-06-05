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

package com.tencent.devops.process.api.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactQualityMetadataResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_UPDATE_FAILED
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.websocket.service.PipelineWebsocketService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineRuntimeResourceImpl @Autowired constructor(
    private val client: Client,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val pipelineWebsocketService: PipelineWebsocketService
) : ServicePipelineRuntimeResource {

    override fun updateArtifactList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryFileList: List<FileInfo>
    ): Result<BuildHistory> {
        val artifactAnalyticsList = getArtifactoryAnalyticsList(
            userId = userId,
            projectId = projectId,
            artifactoryFileList = artifactoryFileList
        )

        val success = pipelineRuntimeService.updateArtifactList(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactListJsonString = JsonUtil.toJson(artifactoryFileList, formatted = false),
            artifactAnalyticsList = JsonUtil.toJson(artifactAnalyticsList, formatted = false),
        )

        if (success) {

            val buildHistory = pipelineRuntimeService.getBuildHistoryById(projectId, buildId)
                ?: throw ErrorCodeException(
                    errorCode = ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )

            webSocketDispatcher.dispatch(
                pipelineWebsocketService.buildHistoryMessage(
                    buildId = buildId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId
                )
            )
            return Result(buildHistory)
        }

        throw ErrorCodeException(
            errorCode = ERROR_UPDATE_FAILED,
            params = arrayOf(buildId)
        )
    }

    fun getArtifactoryAnalyticsList(
        userId: String,
        projectId: String,
        artifactoryFileList: List<FileInfo>
    ): List<ArtifactQualityMetadataAnalytics> {
        val metadataLabels = client.get(ServiceArtifactQualityMetadataResource::class).listArtifactQualityMetadataLabel(
            userId = userId,
            projectId = projectId
        ).data.orEmpty()
        if (metadataLabels.isEmpty() || artifactoryFileList.isEmpty())
            return emptyList()

        // 1. 创建标签键到元数据的快速映射
        val labelMap = metadataLabels.associateBy { it.labelKey }

        // 2. 创建分析结果容器：Map<Pair<标签键, 值>, 计数器>
        val analyticsMap = mutableMapOf<Pair<String, String>, Int>()

        // 3. 遍历所有制品属性
        artifactoryFileList.mapNotNull { it.properties }.forEach { properties ->
            properties.forEach { (key, value) ->
                // 4. 只处理在元数据标签中定义的键
                labelMap[key]?.let {
                    val pair = key to value
                    analyticsMap[pair] = analyticsMap.getOrDefault(pair, 0) + 1
                }
            }
        }

        // 5. 转换为最终结果结构
        return analyticsMap.map { (pair, count) ->
            val (labelKey, value) = pair
            val labelDetail = labelMap[labelKey]!!
            ArtifactQualityMetadataAnalytics(
                labelKey = labelKey,
                value = value,
                color = labelDetail.labelColorMap[value],
                count = count
            )
        }
    }
}
