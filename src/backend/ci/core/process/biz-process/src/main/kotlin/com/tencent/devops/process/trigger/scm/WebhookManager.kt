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
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SOURCE_WEBHOOK
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import com.tencent.devops.repository.pojo.webhook.WebhookData
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitPushHook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestHook
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebhookManager @Autowired constructor(
    private val client: Client,
    private val webhookListeners: List<WebHookEventListener>,
    private val pipelineTriggerEventService: PipelineTriggerEventService
) {

    fun handleRequestEvent(event: ScmWebhookRequestEvent) {
        with(event) {
            val requestId = MDC.get(TraceTag.BIZID)
            try {
                logger.info(
                    "Start to handle webhook request|scmCode:$scmCode|headers:${request.headers}|body:${request.body}"
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
                    "webhook request body parsed|webhookData:${JsonUtil.toJson(webhookData.webhook, false)}"
                )
                // 保存原始报文, 事件回放时第三方触发器有用到
                saveScmWebhookRequest(
                    requestId = requestId,
                    webhookData = webhookData,
                    event = event
                )
                handleWebhookData(
                    requestId = requestId,
                    scmCode = scmCode,
                    webhookData = webhookData,
                    sourceWebhook = request.body
                )
            } catch (ignored: Exception) {
                logger.error("Failed to handle webhook request|scmCode:$scmCode", ignored)
            }
        }
    }

    private fun handleWebhookData(
        requestId: String,
        scmCode: String,
        webhookData: WebhookData,
        sourceWebhook: String
    ) {
        with(webhookData) {
            logger.info(
                "handle webhook data|scmCode:$scmCode|eventType:${webhook.eventType}|" +
                        "repos:${repositories.map { it.repoHashId} }"
            )
            val eventDesc = with(webhook.eventDesc) {
                I18Variable(
                    code = code,
                    params = params,
                    defaultMessage = defaultMessage
                ).toJsonStr()
            }
            repositories.forEach { repository ->
                var eventId = pipelineTriggerEventService.getEventIdOrNull(
                    projectId = repository.projectId!!,
                    requestId = requestId,
                    eventSource = repository.repoHashId!!
                )
                if (eventId == null) {
                    eventId = pipelineTriggerEventService.getEventId()
                    val triggerEvent = PipelineTriggerEvent(
                        projectId = repository.projectId,
                        eventId = eventId,
                        triggerType = repository.getScmType().name,
                        eventSource = repository.repoHashId,
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
                        projectId = repository.projectId!!,
                        eventId = eventId
                    )
                    val triggerEvent = PipelineTriggerEvent(
                        projectId = repository.projectId,
                        eventId = eventId,
                        triggerType = repository.getScmType().name,
                        eventSource = repository.repoHashId,
                        eventType = webhook.eventType,
                        triggerUser = webhook.userName,
                        eventDesc = eventDesc,
                        requestId = requestId,
                        createTime = event?.createTime ?: LocalDateTime.now(),
                        eventBody = webhook
                    )
                    pipelineTriggerEventService.updateTriggerEvent(triggerEvent = triggerEvent)
                }
                fireEvent(
                    eventId = eventId,
                    repository = repository,
                    webhook = webhook,
                    sourceWebhook = sourceWebhook
                )
            }
        }
    }

    /**
     * 触发webhook事件
     * @param eventId 事件ID
     * @param repository 关联仓库
     * @param webhook 解析后的webhook
     * @param replayPipelineId 指定流水线回放
     * @param sourceWebhook webhook原始报文, 第三方触发器要用
     */
    fun fireEvent(
        eventId: Long,
        repository: Repository,
        webhook: Webhook,
        replayPipelineId: String? = null,
        sourceWebhook: String
    ) {
        // 保存原始webhook数据，用于第三方触发器回调
        fillSourceWebhook(webhook = webhook, sourceWebhook = sourceWebhook)
        fireEvent(eventId = eventId, repository = repository, webhook = webhook, replayPipelineId = replayPipelineId)
    }

    /**
     * 触发webhook事件
     *
     * @param replayPipelineId 重放流水线ID, 可能是流水线ID也可能是yaml文件名
     */
    private fun fireEvent(eventId: Long, repository: Repository, webhook: Webhook, replayPipelineId: String? = null) {
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

    private fun fillSourceWebhook(
        webhook: Webhook,
        sourceWebhook: String
    ) {
        when (webhook) {
            is GitPushHook -> {
                webhook.extras[BK_REPO_SOURCE_WEBHOOK] = sourceWebhook
            }

            is PullRequestHook -> {
                webhook.extras[BK_REPO_SOURCE_WEBHOOK] = sourceWebhook
            }
        }
    }

    private fun saveScmWebhookRequest(
        requestId: String,
        webhookData: WebhookData,
        event: ScmWebhookRequestEvent
    ) {
        val webhook = webhookData.webhook
        val repositories = webhookData.repositories
        if (repositories.isEmpty()) {
            logger.warn("The repository associated with the webhook is empty")
            return
        }
        try {
            client.get(ServiceRepositoryWebhookResource::class).saveWebhookRequest(
                repositoryWebhookRequest = RepositoryWebhookRequest(
                    requestId = requestId,
                    externalId = webhook.repository().id.toString(),
                    eventType = webhook.eventType,
                    triggerUser = webhook.userName,
                    eventMessage = "",
                    repositoryType = repositories.first().getScmType().name,
                    requestHeader = event.request.headers,
                    requestParam = event.request.queryParams,
                    requestBody = event.request.body,
                    createTime = LocalDateTime.now()
                )
            ).data!!
        } catch (ignored: Exception) {
            // 日志保存异常,不影响正常触发
            logger.warn("Failed to save webhook request", ignored)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookManager::class.java)
    }
}
