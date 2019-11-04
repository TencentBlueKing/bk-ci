/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.mq

import com.tencent.devops.artifactory.Constants
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.service.ArtifactoryInfoService
import com.tencent.devops.artifactory.service.ArtifactorySearchService
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.process.api.service.ServicePipelineRuntimeResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineBuildExtListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val artifactoryInfoService: ArtifactoryInfoService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val client: Client
) : BaseListener<PipelineBuildFinishBroadCastEvent>(pipelineEventDispatcher) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)!!
    }

    override fun run(event: PipelineBuildFinishBroadCastEvent) {

        val projectId = event.projectId
        val buildId = event.buildId
        val pipelineId = event.pipelineId

        val startTime = System.currentTimeMillis()
        val artifactList: List<FileInfo> = try {
            getArtifactList(projectId, pipelineId, buildId)
        } catch (e: Throwable) {
            logger.error("[$pipelineId]|getArtifactList-$buildId exception:", e)
            emptyList()
        }
        logCostCall(startTime, buildId)
        logger.info("[$pipelineId]|getArtifactList-$buildId artifact: ${JsonUtil.toJson(artifactList)}")

        try {
            if (artifactList.isEmpty()) {
                return
            }

            val result = client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
                userId = event.userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                artifactoryFileList = artifactList
            )

            logger.info("[$buildId]|update artifact result: ${result.status} ${result.message}")

            if (result.isOk() && result.data != null) {

                synArtifactoryInfo(
                    userId = event.userId,
                    artifactList = artifactList as List<FileInfo>,
                    projectId = buildId,
                    pipelineId = pipelineId,
                    buildId = projectId,
                    buildNum = result.data!!.buildNum ?: 0
                )
            }
        } catch (e: Exception) {
            logger.error("[$buildId| update artifact list fail: ${e.localizedMessage}", e)
            // rollback
            client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
                userId = event.userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                artifactoryFileList = emptyList()
            )
        }
    }

    fun getArtifactList(projectId: String, pipelineId: String, buildId: String): List<FileInfo> {
        val fileInfoList = mutableListOf<FileInfo>()

        val propertyList = mutableListOf<Property>()
        propertyList.add(Property("pipelineId", pipelineId))
        propertyList.add(Property("buildId", buildId))

        fileInfoList.addAll(
            artifactorySearchService.serviceSearchFileAndProperty(
                projectId = projectId,
                searchProps = propertyList
            ).second
        )
        logger.info("ArtifactFileList size: ${fileInfoList.size}")
        return fileInfoList.sorted()
    }

    // 备份apk，ipa信息
    fun synArtifactoryInfo(
        userId: String,
        artifactList: List<FileInfo>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNum: Int
    ) {
        try {

            artifactList.forEach { fileInfo ->
                artifactoryInfoService.createInfo(
                    buildId = buildId,
                    pipelineId = pipelineId,
                    projectId = projectId,
                    buildNum = buildNum,
                    fileInfo = fileInfo,
                    dataFrom = Constants.SYN_DATA_FROM_NEW
                )
            }
        } catch (ex: Exception) {
            logger.warn("[$pipelineId]|[$buildId]|syn artifactory fail", ex)
        }
    }

    fun logCostCall(startTime: Long, buildId: String) {
        val cost = System.currentTimeMillis() - startTime
        if (cost > 2000) {
            logger.warn("$buildId - getArtifactList cost:$cost")
        } else if (cost > 5000) {
            logger.error("$buildId - getArtifactList cost:$cost")
        }
    }
}
