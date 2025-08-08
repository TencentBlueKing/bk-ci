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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookTriggerService(
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val pipelineYamlService: PipelineYamlService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebhookTriggerService::class.java)
    }

    fun trigger(
        scmType: ScmType,
        matcher: ScmWebhookMatcher,
        requestId: String,
        eventTime: LocalDateTime
    ) {
        val preMatch = matcher.preMatch()
        if (!preMatch.isMatch) {
            logger.info("webhook trigger pre match|${preMatch.reason}")
            return
        }
        val triggerEvent = PipelineTriggerEvent(
            requestId = requestId,
            triggerType = scmType.name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            createTime = eventTime
        )
        // yaml流水线在yaml触发时触发
        val yamlPipelineIds = getYamlPipelineIds(matcher, scmType)
        val triggerPipelines = pipelineWebhookService.getTriggerPipelines(
            name = matcher.getRepoName(),
            repositoryType = scmType,
            yamlPipelineIds = yamlPipelineIds,
            compatibilityRepoNames = matcher.getCompatibilityRepoName()
        )
        pipelineBuildWebhookService.dispatchTriggerPipelines(
            matcher = matcher,
            triggerEvent = triggerEvent,
            triggerPipelines = triggerPipelines
        )
    }

    fun getYamlPipelineIds(
        matcher: ScmWebhookMatcher,
        scmType: ScmType
    ): List<String> {
        try {
            val repository = client.get(ServiceRepositoryPacResource::class).getPacRepository(
                externalId = matcher.getExternalId(), scmType = scmType
            ).data
            if (repository == null || repository.projectId.isNullOrBlank() || repository.repoHashId.isNullOrBlank()) {
                return emptyList()
            }
            return pipelineYamlService.getAllYamlPipeline(
                projectId = repository.projectId!!,
                repoHashId = repository.repoHashId!!
            ).map { it.pipelineId }
        } catch (ignored: Exception) {
            logger.info("get yaml pipelineId error when webhook trigger", ignored)
            return emptyList()
        }
    }

    fun replay(
        replayEvent: ReplayWebhookEvent,
        triggerEvent: PipelineTriggerEvent,
        matcher: ScmWebhookMatcher
    ) {
        val preMatch = matcher.preMatch()
        if (!preMatch.isMatch) {
            logger.info("webhook replay trigger pre match|${preMatch.reason}")
            return
        }

        val triggerPipelines = with(replayEvent) {
            pipelineId?.let {
                listOf(
                    WebhookTriggerPipeline(
                        projectId = projectId,
                        pipelineId = pipelineId
                    )
                )
            } ?: run {
                pipelineWebhookService.listTriggerPipeline(
                    projectId = projectId,
                    repositoryHashId = triggerEvent.eventSource!!,
                    eventType = triggerEvent.eventType
                )
            }
        }
        pipelineBuildWebhookService.dispatchTriggerPipelines(
            matcher = matcher,
            triggerEvent = triggerEvent,
            triggerPipelines = triggerPipelines
        )
    }
}
