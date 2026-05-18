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
 */

package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.service.CreateStreamTriggerSupportService
import com.tencent.devops.process.trigger.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * TAPD 事件触发构建服务
 */
@Service
class TapdEventTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val tapdEventMatcher: TapdEventTriggerMatcher,
    private val createStreamTriggerSupportService: CreateStreamTriggerSupportService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdEventTriggerBuildService::class.java)
    }

    fun tapdWebhookTrigger(event: TapdWebhookTriggerEvent) {
        logger.info("Receive tapd webhook trigger event[${JsonUtil.toJson(event, false)}]")
        val context = WebhookTriggerContext(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            eventId = event.eventId,
            startType = StartType.WEB_HOOK
        )
        try {
            doTrigger(event, context)
        } catch (ignored: Exception) {
            logger.warn(
                "failed to trigger by tapd webhook|${event.projectId}|${event.pipelineId}|${event.tapdProjectId}",
                ignored
            )
            webhookTriggerManager.fireError(context, ignored)
        }
    }

    private fun doTrigger(event: TapdWebhookTriggerEvent, context: WebhookTriggerContext) = with(event) {
        // 1. 解析事件类型与动作
        val tapdEventType = TapdEventType.parse(eventType) ?: run {
            logger.warn("Unsupported tapd event type|$eventType")
            return@with
        }
        val tapdEventAction = TapdEventAction.parse(eventAction) ?: run {
            logger.warn("Unsupported tapd event action|$eventAction")
            return@with
        }
        // 2. 查询流水线信息，已锁定的流水线跳过
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        if (pipelineInfo.locked == true) return@with
        context.pipelineInfo = pipelineInfo

        // 3. 创作环境下需要补充节点相关启动参数
        val externalStartParams = externalStartParams(pipelineInfo)

        // 4. 匹配触发器并启动构建
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = null
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        matchAndStart(
            event = this,
            context = context,
            pipelineInfo = pipelineInfo,
            resource = resource,
            tapdEventType = tapdEventType,
            tapdEventAction = tapdEventAction,
            externalStartParams = externalStartParams
        )
    }

    /**
     * 补充扩展参数
     */
    private fun TapdWebhookTriggerEvent.externalStartParams(
        pipelineInfo: PipelineInfo
    ): Map<String, String> {
        if (pipelineInfo.channelCode != ChannelCode.CREATIVE_STREAM) return mapOf()

        val envHashId = pipelineRepositoryService.getSetting(projectId, pipelineId)?.envHashId ?: ""
        val oauthUserId = pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
            ?: pipelineInfo.lastModifyUser
        val agentHashId = createStreamTriggerSupportService.getEnvNodeList(
            projectId = projectId,
            envHashId = envHashId,
            userId = oauthUserId
        ).firstOrNull() ?: run {
            logger.warn("skip trigger $pipelineId|no available node found in env[$envHashId] of project[$projectId]")
            return mapOf()
        }
        return createStreamTriggerSupportService.creativeStreamParams(
            projectId = projectId,
            agentHashId = agentHashId,
            userId = oauthUserId
        )
    }

    @Suppress("LongParameterList")
    private fun matchAndStart(
        event: TapdWebhookTriggerEvent,
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        tapdEventType: TapdEventType,
        tapdEventAction: TapdEventAction,
        externalStartParams: Map<String, String>
    ) {
        val elements = resource.model.getTriggerContainer().elements
            .filterIsInstance<TapdWebHookTriggerElement>()
            .filter { it.elementEnabled() }
        if (elements.isEmpty()) {
            logger.info("no enabled tapd trigger element|${event.projectId}|${event.pipelineId}")
            return
        }

        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        for (element in elements) {
            val atomResponse = tapdEventMatcher.matches(
                element = element,
                tapdProjectId = event.tapdProjectId,
                eventType = tapdEventType,
                eventAction = tapdEventAction,
                triggerUser = event.triggerUser
            )
            when (atomResponse.matchStatus) {
                MatchStatus.SUCCESS -> {
                    webhookTriggerBuildService.startPipeline(
                        context = context,
                        pipelineInfo = pipelineInfo,
                        resource = resource,
                        startParams = event.startParams.plus(externalStartParams)
                    )
                    logger.info(
                        "tapd webhook trigger success|${event.projectId}|${event.pipelineId}|element=${element.id}"
                    )
                    return
                }
                MatchStatus.CONDITION_NOT_MATCH -> failedMatchElements.add(
                    PipelineTriggerFailedMatchElement(
                        elementId = element.id,
                        elementName = element.name,
                        elementAtomCode = element.getAtomCode(),
                        reasonMsg = atomResponse.failedReason
                            ?: I18Variable(code = TRIGGER_CONDITION_NOT_MATCH).toJsonStr()
                    )
                )
                else -> Unit
            }
        }
        if (failedMatchElements.isNotEmpty()) {
            context.failedMatchElements = failedMatchElements
            webhookTriggerManager.fireMatchFailed(context)
        }
    }
}
