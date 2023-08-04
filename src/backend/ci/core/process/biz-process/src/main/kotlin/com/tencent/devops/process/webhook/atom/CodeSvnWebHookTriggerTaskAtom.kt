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
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.webhook.atom.IWebhookAtomTask
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.WebhookRequestReplay
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.service.code.ScmWebhookMatcherBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.webhook.PipelineWebhookSubscriber
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CodeSvnWebHookTriggerTaskAtom (
    private val objectMapper: ObjectMapper,
    private val scmWebhookMatcherBuilder: ScmWebhookMatcherBuilder,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val webhookTriggerTaskAtomService: WebhookTriggerTaskAtomService
) : IWebhookAtomTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeSvnWebHookTriggerTaskAtom::class.java)
    }

    override fun request(request: WebhookRequest) {
        logger.info("Trigger code svn build - ${request.body}")

        val event = getEvent(request = request) ?: return

        val matcher = scmWebhookMatcherBuilder.createSvnWebHookMatcher(event)

        val eventTime = LocalDateTime.now()
        val requestId = webhookTriggerTaskAtomService.saveRepoWebhookRequest(
            matcher = matcher,
            request = request,
            eventTime = eventTime,
            scmType = ScmType.CODE_SVN
        )
        val triggerEvent = PipelineTriggerEvent(
            triggerType = PipelineTriggerType.CODE_SVN.name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            hookRequestId = requestId,
            eventTime = eventTime
        )

        val subscribers = pipelineWebhookService.getWebhookPipelines(
            name = matcher.getRepoName(),
            repositoryType = ScmType.CODE_SVN.name
        )
        pipelineBuildWebhookService.dispatchPipelineSubscribers(
            matcher = matcher,
            triggerEvent = triggerEvent,
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
                body = repoWebhookRequest.requestBody
            )
        ) ?: return
        val matcher = scmWebhookMatcherBuilder.createSvnWebHookMatcher(event)
        val eventTime = LocalDateTime.now()
        val triggerEvent = PipelineTriggerEvent(
            triggerType = PipelineTriggerType.CODE_SVN.name,
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
                repositoryType = ScmType.CODE_SVN.name
            )
        }
        pipelineBuildWebhookService.dispatchPipelineSubscribers(
            matcher = matcher,
            triggerEvent = triggerEvent,
            subscribers = subscribers
        )
    }

    private fun getEvent(request: WebhookRequest): SvnCommitEvent? {
        return try {
            objectMapper.readValue(request.body, SvnCommitEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Fail to parse the svn web hook commit event", e)
            return null
        }
    }
}
