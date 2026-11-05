/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.process.trigger

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.api.service.ServiceWebhookBuildResource
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.webhook.WebhookStartPipelineRequest
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WebhookTriggerBuildService(
    private val triggerProcessClient: TriggerProcessClient,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineRepositoryService: PipelineRepositoryService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebhookTriggerBuildService::class.java)
    }

    fun startPipeline(
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>
    ) {
        val buildId = startPipeline(
            pipelineInfo = pipelineInfo,
            resource = resource,
            startParams = startParams,
            startType = context.startType
        )
        context.buildId = buildId
        context.startParams = startParams
        webhookTriggerManager.fireBuildSuccess(context = context)
    }

    fun startPipeline(
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>,
        startType: StartType = StartType.SERVICE
    ): BuildId {
        val startEpoch = System.currentTimeMillis()
        val (projectId, pipelineId) =
            pipelineInfo.projectId to pipelineInfo.pipelineId
        val userId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val buildId = triggerProcessClient.call(projectId, ServiceWebhookBuildResource::class) {
            it.webhookStartPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                request = WebhookStartPipelineRequest(
                    startType = startType,
                    version = resource.version,
                    startParams = startParams,
                    frequencyLimit = false
                )
            ).data
        } ?: throw IllegalStateException("webhook start pipeline returned empty buildId|$projectId|$pipelineId")
        logger.info(
            "success to start pipeline|" +
                "projectId: $projectId|pipelineId: $pipelineId|" +
                "version: ${resource.version}|" +
                "time=${System.currentTimeMillis() - startEpoch}"
        )
        return buildId
    }
}
