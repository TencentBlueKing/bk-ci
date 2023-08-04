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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.webhook.atom.IWebhookAtomTask
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.WebhookRequestReplay
import com.tencent.devops.common.webhook.pojo.code.github.GithubCheckRunEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.api.service.ServiceBuildResource
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
class CodeGithubWebHookTriggerTaskAtom(
    private val objectMapper: ObjectMapper,
    private val scmWebhookMatcherBuilder: ScmWebhookMatcherBuilder,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val webhookTriggerTaskAtomService: WebhookTriggerTaskAtomService,
    private val client: Client
) : IWebhookAtomTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGithubWebHookTriggerTaskAtom::class.java)
    }

    override fun request(request: WebhookRequest) {
        val event = getEvent(request) ?: return
        val matcher = scmWebhookMatcherBuilder.createGithubWebHookMatcher(event)
        if (!matcher.preMatch().isMatch) {
            return
        }
        if (event is GithubCheckRunEvent) {
            retry(event)
            return
        }
        val eventTime = LocalDateTime.now()
        val requestId = webhookTriggerTaskAtomService.saveRepoWebhookRequest(
            matcher = matcher,
            request = request,
            eventTime = eventTime,
            scmType = ScmType.GITHUB
        )
        val webhookEvent = PipelineTriggerEvent(
            triggerType = PipelineTriggerType.GITHUB.name,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            hookRequestId = requestId,
            eventTime = eventTime
        )
        val subscribers = pipelineWebhookService.getWebhookPipelines(
            name = matcher.getRepoName(),
            repositoryType = ScmType.CODE_GIT.name
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
        val matcher = scmWebhookMatcherBuilder.createGithubWebHookMatcher(event)
        val eventTime = LocalDateTime.now()
        val triggerEvent = PipelineTriggerEvent(
            triggerType = PipelineTriggerType.GITHUB.name,
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
                repositoryType = ScmType.GITHUB.name
            )
        }
        pipelineBuildWebhookService.dispatchPipelineSubscribers(
            matcher = matcher,
            triggerEvent = triggerEvent,
            subscribers = subscribers
        )
    }

    private fun retry(event: GithubCheckRunEvent) {
        if (event.action != "rerequested") {
            logger.info("Unsupported check run action:${event.action}")
            return
        }
        if (event.checkRun.externalId == null) {
            logger.info("github check run externalId is empty")
            return
        }
        val buildInfo = event.checkRun.externalId!!.split("_")
        if (buildInfo.size < 4) {
            logger.info("the buildInfo of github check run is error")
            return
        }
        client.get(ServiceBuildResource::class).retry(
            userId = buildInfo[0],
            projectId = buildInfo[1],
            pipelineId = buildInfo[2],
            buildId = buildInfo[3],
            channelCode = ChannelCode.BS
        )
    }

    private fun getEvent(request: WebhookRequest): GithubEvent? {
        val eventType = request.headers?.get("X-GitHub-Event")
        val guid = request.headers?.get("X-Github-Delivery")
        val signature = request.headers?.get("X-Hub-Signature")
        val body = request.body
        logger.info("Trigger code github build (event=$eventType, guid=$guid, signature=$signature, body=$body)")

        return when (eventType) {
            GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(body)
            GithubCreateEvent.classType -> objectMapper.readValue<GithubCreateEvent>(body)
            GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(body)
            GithubCheckRunEvent.classType -> objectMapper.readValue<GithubCheckRunEvent>(body)
            else -> {
                logger.info("Github event($eventType) is ignored")
                return null
            }
        }
    }
}
