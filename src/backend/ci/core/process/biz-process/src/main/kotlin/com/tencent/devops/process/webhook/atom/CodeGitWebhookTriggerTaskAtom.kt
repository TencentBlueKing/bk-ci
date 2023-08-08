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
 *
 */

package com.tencent.devops.process.webhook.atom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.webhook.atom.IWebhookAtomTask
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.WebhookRequestReplay
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.service.code.ScmWebhookMatcherBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.webhook.PipelineWebhookSubscriber
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CodeGitWebhookTriggerTaskAtom @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val scmWebhookMatcherBuilder: ScmWebhookMatcherBuilder,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val webhookTriggerTaskAtomService: WebhookTriggerTaskAtomService
) : IWebhookAtomTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitWebhookTriggerTaskAtom::class.java)
    }

    override fun request(request: WebhookRequest) {
        val eventType = request.headers?.get("X-Event")
        val body = request.body
        logger.info("Trigger code git build($body|$eventType)")
        val event = getEvent(request = request) ?: return
        val matcher = scmWebhookMatcherBuilder.createGitWebHookMatcher(event)
        if (!matcher.preMatch().isMatch) {
            return
        }

        val eventTime = LocalDateTime.now()
        val requestId = webhookTriggerTaskAtomService.saveRepoWebhookRequest(
            matcher = matcher,
            request = request,
            eventTime = eventTime,
            scmType = getScmType()
        )
        val webhookEvent = PipelineTriggerEvent(
            triggerType = getScmType().name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            hookRequestId = requestId,
            eventTime = eventTime
        )
        val subscribers = pipelineWebhookService.getWebhookPipelines(
            name = matcher.getRepoName(),
            repositoryType = getScmType().name
        )
        pipelineBuildWebhookService.dispatchPipelineSubscribers(
            matcher = matcher,
            triggerEvent = webhookEvent,
            subscribers = subscribers
        )
    }

    override fun replay(request: WebhookRequestReplay) {
        val repoWebhookRequest = webhookTriggerTaskAtomService.getRepoWebhookRequest(
            requestId = request.hookRequestId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_WEBHOOK_REQUEST_NOT_FOUND,
            params = arrayOf(request.hookRequestId.toString())
        )
        val event = getEvent(
            request = WebhookRequest(
                headers = repoWebhookRequest.requestHeader,
                body = repoWebhookRequest.requestBody
            )
        ) ?: return
        val matcher = scmWebhookMatcherBuilder.createGitWebHookMatcher(event)
        val eventTime = LocalDateTime.now()
        val triggerEvent = PipelineTriggerEvent(
            triggerType = getScmType().name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            hookRequestId = request.hookRequestId,
            eventTime = eventTime
        )
        val subscribers = request.pipelineId?.let {
            listOf(
                PipelineWebhookSubscriber(
                    projectId = request.projectId,
                    pipelineId = request.pipelineId!!
                )
            )
        } ?: run {
            pipelineWebhookService.getWebhookPipelines(
                name = matcher.getRepoName(),
                repositoryType = getScmType().name
            )
        }
        pipelineBuildWebhookService.dispatchPipelineSubscribers(
            matcher = matcher,
            triggerEvent = triggerEvent,
            subscribers = subscribers
        )
    }

    private fun getEvent(request: WebhookRequest): GitEvent? {
        val eventType = request.headers?.get("X-Event")
        val body = request.body
        return try {
            if (eventType == "Review Hook") {
                objectMapper.readValue<GitReviewEvent>(body)
            } else {
                objectMapper.readValue<GitEvent>(body)
            }
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event", e)
            return null
        }
    }

    fun getScmType() = ScmType.CODE_GIT
}
