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

package com.tencent.devops.artifactory.mq

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.service.PipelineBuildArtifactoryService
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineRuntimeResource
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ALL")
class PipelineBuildArtifactoryListener @Autowired constructor(
    private val pipelineBuildArtifactoryService: PipelineBuildArtifactoryService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildArtifactoryListener::class.java)!!
    }

    fun onBuildFinished(event: PipelineBuildFinishBroadCastEvent) {
        logger.info("PipelineBuildArtifactoryListener.run, event: $event")
        val userId = event.userId
        val projectId = event.projectId
        val buildId = event.buildId
        val pipelineId = event.pipelineId

        updateArtifactList(userId, projectId, pipelineId, buildId)

        val (parentProjectId, parentPipelineId, parentBuildId) = getParentPipelineVars(
            userId, projectId, pipelineId, buildId
        )
        if (parentProjectId.isNotBlank() && parentPipelineId.isNotBlank() && parentBuildId.isNotBlank()) {
            updateArtifactList(userId, parentProjectId, parentPipelineId, parentBuildId)
        }
    }

    private fun getParentPipelineVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Triple<String, String, String> {
        val parentPipelineVars = try {
            client.get(ServiceBuildResource::class).getBuildVariableValue(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variableNames = listOf(
                    PIPELINE_START_PARENT_PROJECT_ID,
                    PIPELINE_START_PARENT_PIPELINE_ID,
                    PIPELINE_START_PARENT_BUILD_ID
                )
            ).data!!
        } catch (ignore: RemoteServiceException) {
            if (ignore.httpStatus >= 500) {
                logger.error("BKSystemErrorMonitor|getBuildVariableValue|$pipelineId-$buildId|${ignore.errorMessage}")
            } else {
                logger.info("getBuildVariableValue|$pipelineId-$buildId|${ignore.errorMessage}")
            }
            return Triple("", "", "")
        }
        logger.info("[$pipelineId|$buildId] get parent pipeline vars: $parentPipelineVars")
        val parentProjectId = parentPipelineVars[PIPELINE_START_PARENT_PROJECT_ID].toString()
        val parentPipelineId = parentPipelineVars[PIPELINE_START_PARENT_PIPELINE_ID].toString()
        val parentBuildId = parentPipelineVars[PIPELINE_START_PARENT_BUILD_ID].toString()
        return Triple(parentProjectId, parentPipelineId, parentBuildId)
    }

    private fun updateArtifactList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        val artifactList: List<FileInfo> = try {
            pipelineBuildArtifactoryService.getArtifactList(userId, projectId, pipelineId, buildId)
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|getArtifactList|$pipelineId-$buildId|error=${ignored.message}", ignored)
            emptyList()
        }
        logger.info("[$pipelineId]|getArtifactList-$buildId artifact: ${JsonUtil.toJson(artifactList)}")

        try {
            if (artifactList.isEmpty()) {
                return
            }

            val result = client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                artifactoryFileList = artifactList
            )

            logger.info("[$buildId]|update artifact result: ${result.status} ${result.message}")
        } catch (e: Exception) {
            logger.error("BKSystemErrorMonitor|updateArtifactList|$buildId|error=${e.localizedMessage}", e)
            // rollback
            client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                artifactoryFileList = emptyList()
            )
        }
    }
}
