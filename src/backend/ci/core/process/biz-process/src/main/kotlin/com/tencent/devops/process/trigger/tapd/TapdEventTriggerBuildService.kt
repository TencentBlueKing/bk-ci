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
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.service.CreateStreamTriggerSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
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
 *
 * 与 [TapdWebhookRequestService] 分工：
 * - RequestService：解析原始 webhook、保存 [com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent]、
 *   投递精简版 [TapdWebhookTriggerEvent] 路由事件。
 * - BuildService：接收路由事件后反查 `PipelineTriggerEvent.eventBody` 拿到原始 body，
 *   完成触发器匹配 → 匹配成功再组装启动参数 → 启动流水线。
 *
 * 该分工与 SCM/Market 触发保持一致（参考 `ScmWebhookTriggerBuildService.trigger`）。
 */
@Service
class TapdEventTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val tapdEventMatcher: TapdEventTriggerMatcher,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
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
                "failed to trigger by tapd webhook|${event.projectId}|${event.pipelineId}|${event.workspaceId}",
                ignored
            )
            webhookTriggerManager.fireError(context, ignored)
        }
    }

    private fun doTrigger(event: TapdWebhookTriggerEvent, context: WebhookTriggerContext) = with(event) {
        // 1. 反查触发事件（参考 webhook 那边），从 eventBody 取原始 body
        val triggerEvent = pipelineTriggerEventService.getTriggerEvent(projectId, eventId) ?: run {
            logger.info("tapd trigger event not found|$eventId|$projectId|$pipelineId")
            return@with
        }
        val body = (triggerEvent.eventBody as? GenericWebhookEventBody)?.body ?: run {
            logger.info("tapd trigger event body is empty|$eventId|$projectId|$pipelineId")
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

        // 3. 取流水线资源版本
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = null
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

        // 4. 匹配触发器并启动构建（启动参数在匹配成功后再计算）
        matchAndStart(
            event = this,
            context = context,
            pipelineInfo = pipelineInfo,
            resource = resource,
            body = body
        )
    }

    @Suppress("LongParameterList")
    private fun matchAndStart(
        event: TapdWebhookTriggerEvent,
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        body: Map<String, String>
    ) {
        val elements = resource.model.getTriggerContainer().elements
            .filterIsInstance<TapdWebHookTriggerElement>()
            .filter { it.elementEnabled() }
        if (elements.isEmpty()) {
            logger.info("no enabled tapd trigger element|${event.projectId}|${event.pipelineId}")
            return
        }

        val variables = pipelineRepositoryService.getTriggerParams(resource.model.getTriggerContainer())
        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        for (element in elements) {
            val atomResponse = tapdEventMatcher.matches(
                element = element,
                event = event,
                body = body,
                variables = variables
            )
            when (atomResponse.matchStatus) {
                MatchStatus.SUCCESS -> {
                    startPipeline(
                        event = event,
                        context = context,
                        pipelineInfo = pipelineInfo,
                        resource = resource,
                        element = element,
                        body = body
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

    /**
     * 匹配成功后组装启动参数并启动流水线
     *
     * 启动参数由两部分组成：
     * 1. [TapdWebhookUtils.buildStartParams]：基于 event + body 计算 TAPD 事件的启动变量；
     * 2. [CreateStreamTriggerSupportService.externalWebhookStartParams]：创作流场景下的额外节点参数。
     *
     * 二者都在这里延迟计算，匹配失败的路径不会产生任何组装开销。
     */
    private fun startPipeline(
        event: TapdWebhookTriggerEvent,
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        element: Element,
        body: Map<String, String>
    ) {
        val tapdParams = TapdWebhookUtils.buildStartParams(
            event = event,
            body = body,
            element = element
        )
        val oauthUserId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = event.projectId,
            pipelineId = event.pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val externalStartParams = createStreamTriggerSupportService.externalWebhookStartParams(
            pipelineInfo = pipelineInfo,
            userId = oauthUserId
        )
        webhookTriggerBuildService.startPipeline(
            context = context,
            pipelineInfo = pipelineInfo,
            resource = resource,
            startParams = tapdParams + externalStartParams
        )
    }
}
