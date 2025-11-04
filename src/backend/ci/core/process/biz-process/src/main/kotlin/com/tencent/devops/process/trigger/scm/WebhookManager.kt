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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.webhook.WebhookEnrichRequest
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookManager @Autowired constructor(
    private val client: Client,
    private val webhookListeners: List<WebHookEventListener>,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineYamlService: PipelineYamlService
) {

    fun handleRequestEvent(event: ScmWebhookRequestEvent) {
        with(event) {
            val requestId = MDC.get(TraceTag.BIZID)
            try {
                logger.info(
                    "Start to handle webhook request|$scmCode|${request.headers}|${request.body}"
                )
                val webhookData = client.get(ServiceRepositoryWebhookResource::class).webhookParse(
                    scmCode = scmCode,
                    request = WebhookParseRequest(
                        requestId = requestId,
                        headers = request.headers,
                        queryParams = request.queryParams,
                        body = request.body
                    )
                ).data ?: return
                logger.info(
                    "webhook data parsed successfully|${JsonUtil.toJson(webhookData.webhook, false)}"
                )
                webhookData.repositories.forEach { repository ->
                    handleRepoWebhook(
                        requestId = requestId,
                        repository = repository,
                        webhook = webhookData.webhook
                    )
                }
            } catch (ignored: Exception) {
                logger.error("Failed to handle webhook request|scmCode:$scmCode", ignored)
            }
        }
    }

    private fun handleRepoWebhook(
        requestId: String,
        repository: Repository,
        webhook: Webhook
    ) {
        try {
            // 如果代码库没有绑定任何流水线,则直接跳过
            if (!hasPipelineBinding(repository = repository, eventType = webhook.eventType)) return
            // 解析出来的webhook数据不全,需要调scm接口,将webhook数据补全
            val enWebhook = client.get(ServiceRepositoryWebhookResource::class).webhookEnrich(
                projectId = repository.projectId!!,
                repoHashId = repository.repoHashId!!,
                request = WebhookEnrichRequest(
                    webhook = webhook
                )
            ).data ?: return
            val eventId = saveTriggerEvent(
                requestId = requestId,
                repository = repository,
                webhook = enWebhook
            )
            fireEvent(
                eventId = eventId,
                repository = repository,
                webhook = enWebhook
            )
        } catch (ignored: Exception) {
            logger.error("Failed to handle repo webhook|${repository.repoHashId}|${webhook.eventType}", ignored)
        }
    }

    private fun hasPipelineBinding(repository: Repository, eventType: String): Boolean {
        val hasWebhookBinding = pipelineWebhookService.countTriggerPipeline(
            projectId = repository.projectId!!,
            repositoryHashId = repository.repoHashId!!,
            eventType = eventType
        ) > 0L
        val hashYamlBinding = repository.enablePac?.takeIf { it }?.let {
            pipelineYamlService.countPipelineYaml(
                projectId = repository.projectId!!,
                repoHashId = repository.repoHashId!!
            ) > 0L
        } ?: false
        // 只要有绑定,就触发
        return hasWebhookBinding || hashYamlBinding
    }

    private fun saveTriggerEvent(
        requestId: String,
        repository: Repository,
        webhook: Webhook
    ): Long {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        var eventId = pipelineTriggerEventService.getEventIdOrNull(
            projectId = projectId,
            requestId = requestId,
            eventSource = repoHashId
        )
        val eventDesc = with(webhook.eventDesc) {
            I18Variable(
                code = code,
                params = params,
                defaultMessage = defaultMessage
            ).toJsonStr()
        }
        if (eventId == null) {
            eventId = pipelineTriggerEventService.getEventId()
            val triggerEvent = PipelineTriggerEvent(
                projectId = projectId,
                eventId = eventId,
                triggerType = repository.getScmType().name,
                eventSource = repoHashId,
                eventType = webhook.eventType,
                triggerUser = webhook.userName,
                eventDesc = eventDesc,
                requestId = requestId,
                createTime = LocalDateTime.now(),
                eventBody = webhook
            )
            pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
        } else {
            val event = pipelineTriggerEventService.getTriggerEvent(
                projectId = projectId,
                eventId = eventId
            )
            val triggerEvent = PipelineTriggerEvent(
                projectId = projectId,
                eventId = eventId,
                triggerType = repository.getScmType().name,
                eventSource = repoHashId,
                eventType = webhook.eventType,
                triggerUser = webhook.userName,
                eventDesc = eventDesc,
                requestId = requestId,
                createTime = event?.createTime ?: LocalDateTime.now(),
                eventBody = webhook
            )
            pipelineTriggerEventService.updateTriggerEvent(triggerEvent = triggerEvent)
        }
        return eventId
    }

    /**
     * 触发webhook事件
     * @param eventId 事件ID
     * @param repository 关联仓库
     * @param webhook 解析后的webhook
     * @param replayPipelineId 指定流水线回放
     */
    fun fireEvent(eventId: Long, repository: Repository, webhook: Webhook, replayPipelineId: String? = null) {
        webhookListeners.forEach { listener ->
            try {
                listener.onEvent(
                    eventId = eventId,
                    repository = repository,
                    webhook = webhook,
                    replayPipelineId = replayPipelineId
                )
            } catch (ignored: Exception) {
                logger.error(
                    "Failed to fire event|${repository.projectId}|${repository.repoHashId}|${webhook.eventType}",
                    ignored
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookManager::class.java)
    }
}
