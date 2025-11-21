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
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookTriggerBuildService(
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val pipelineYamlService: PipelineYamlService,
    private val client: Client,
    private val pipelineBuildService: PipelineBuildService,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineRepositoryService: PipelineRepositoryService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebhookTriggerBuildService::class.java)
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

    fun startPipeline(
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>
    ) {
        val startEpoch = System.currentTimeMillis()
        val (projectId, pipelineId) = pipelineInfo.projectId to pipelineInfo.pipelineId
        val userId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val buildId = pipelineBuildService.startPipeline(
            userId = userId,
            pipeline = pipelineInfo,
            startType = StartType.WEB_HOOK,
            pipelineParamMap = convertBuildParameters(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                triggerContainer = resource.model.getTriggerContainer(),
                startParams = startParams
            ),
            channelCode = pipelineInfo.channelCode,
            isMobile = false,
            resource = resource,
            signPipelineVersion = resource.version,
            frequencyLimit = false
        )
        logger.info(
            "success to trigger by webhook|eventId:${context.eventId}|" +
                    "projectId: $projectId|pipelineId: $pipelineId|version: ${resource.version}"
        )
        context.buildId = buildId
        context.startParams = startParams
        webhookTriggerManager.fireBuildSuccess(context = context)
        logger.info("$pipelineId|WEBHOOK_TRIGGER|time=${System.currentTimeMillis() - startEpoch}")
    }

    private fun convertBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerContainer: TriggerContainer,
        startParams: Map<String, Any>
    ): MutableMap<String, BuildParameters> {
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = triggerContainer.params,
            paramValues = startParams.mapValues { it.value.toString() }
        )
        pipelineParamMap.putAll(paramMap)
        startParams.forEach {
            if (paramMap.containsKey(it.key)) {
                return@forEach
            }
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value ?: "")
            } else if (!pipelineParamMap.contains(newVarName)) { // 新变量还不存在，加入
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value ?: "")
            }
        }
        return pipelineParamMap
    }
}
