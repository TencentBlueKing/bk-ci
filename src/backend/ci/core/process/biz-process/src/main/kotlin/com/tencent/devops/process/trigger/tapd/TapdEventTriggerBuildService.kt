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
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
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
 * 消费 [TapdWebhookTriggerEvent]，对**单条流水线**完成：加载资源 → 调 matcher → 启动构建 / 写 detail。
 * 启动参数已由 [TapdWebhookRequestService] 预构建在 [TapdWebhookTriggerEvent.startParams] 中，本服务只需透传。
 */
@Service
class TapdEventTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val tapdEventMatcher: TapdEventTriggerMatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdEventTriggerBuildService::class.java)
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    fun tapdWebhookTrigger(event: TapdWebhookTriggerEvent) {
        logger.info("Receive tapd webhook trigger event[${JsonUtil.toJson(event, false)}]")
        with(event) {
            val context = WebhookTriggerContext(
                projectId = projectId,
                pipelineId = pipelineId,
                eventId = eventId,
                startType = StartType.WEB_HOOK
            )
            try {
                val tapdEventType = TapdEventType.parse(eventType) ?: run {
                    logger.warn("Unsupported tapd event type|$eventType")
                    return
                }
                val tapdEventAction = TapdEventAction.parse(eventAction) ?: run {
                    logger.warn("Unsupported tapd event action|$eventAction")
                    return
                }
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                    projectId = projectId,
                    pipelineId = pipelineId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
                if (pipelineInfo.locked == true) return
                context.pipelineInfo = pipelineInfo

                val resource = pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = null
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                )
                val triggerContainer = resource.model.getTriggerContainer()
                val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
                triggerContainer.elements.filterIsInstance<TapdWebHookTriggerElement>()
                    .forEach elements@{ element ->
                        if (!element.elementEnabled()) {
                            return@elements
                        }
                        val atomResponse = tapdEventMatcher.matches(
                            element = element,
                            tapdProjectId = tapdProjectId,
                            eventType = tapdEventType,
                            eventAction = tapdEventAction,
                            triggerUser = triggerUser
                        )
                        when (atomResponse.matchStatus) {
                            MatchStatus.SUCCESS -> {
                                webhookTriggerBuildService.startPipeline(
                                    context = context,
                                    pipelineInfo = pipelineInfo,
                                    resource = resource,
                                    startParams = startParams
                                )
                                logger.info(
                                    "tapd webhook trigger success|$projectId|$pipelineId|element=${element.id}"
                                )
                                return
                            }

                            MatchStatus.CONDITION_NOT_MATCH -> {
                                failedMatchElements.add(
                                    PipelineTriggerFailedMatchElement(
                                        elementId = element.id,
                                        elementName = element.name,
                                        elementAtomCode = element.getAtomCode(),
                                        reasonMsg = atomResponse.failedReason ?: I18Variable(
                                            code = TRIGGER_CONDITION_NOT_MATCH
                                        ).toJsonStr()
                                    )
                                )
                            }

                            else -> {
                                return@elements
                            }
                        }
                    }
                if (failedMatchElements.isNotEmpty()) {
                    context.failedMatchElements = failedMatchElements
                    webhookTriggerManager.fireMatchFailed(context)
                } else {
                    logger.info("no enabled tapd trigger element|$projectId|$pipelineId")
                }
            } catch (ignored: Exception) {
                logger.error(
                    "Failed to trigger by tapd webhook|$projectId|$pipelineId|$tapdProjectId",
                    ignored
                )
                webhookTriggerManager.fireError(context, ignored)
            }
        }
    }
}
