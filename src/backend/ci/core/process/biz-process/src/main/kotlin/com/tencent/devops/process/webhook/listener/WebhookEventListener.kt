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
 */

package com.tencent.devops.process.webhook.listener

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.engine.service.PipelineWebhookBuildLogContext
import com.tencent.devops.process.engine.service.PipelineWebhookBuildLogService
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ICodeWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.P4WebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.TGitWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.enum.CommitEventType
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class WebhookEventListener constructor(
    private val pipelineBuildService: PipelineBuildWebhookService,
    private val rabbitTemplate: RabbitTemplate,
    private val triggerBuildLogService: PipelineWebhookBuildLogService
) {

    fun handleCommitEvent(event: ICodeWebhookEvent) {
        val traceId = MDC.get(TraceTag.BIZID)
        if (traceId.isNullOrEmpty()) {
            if (!event.traceId.isNullOrEmpty()) {
                MDC.put(TraceTag.BIZID, event.traceId)
            } else {
                MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            }
        }
        logger.info("Receive WebhookEvent from MQ [${event.commitEventType}|${event.requestContent}]|[${event.event}]")
        var result = false
        try {
            PipelineWebhookBuildLogContext.initTriggerLog(
                codeType = event.commitEventType.name,
                requestContent = event.requestContent
            )
            when (event.commitEventType) {
                CommitEventType.SVN -> pipelineBuildService.externalCodeSvnBuild(event.requestContent)
                CommitEventType.GIT -> {
                    pipelineBuildService.externalCodeGitBuild(
                        codeRepositoryType = CodeGitWebHookTriggerElement.classType,
                        event = event.event,
                        body = event.requestContent
                    )
                }
                CommitEventType.GITLAB -> pipelineBuildService.externalGitlabBuild(
                    e = event.requestContent
                )
                CommitEventType.TGIT -> {
                    pipelineBuildService.externalCodeGitBuild(
                        codeRepositoryType = CodeTGitWebHookTriggerElement.classType,
                        event = event.event,
                        body = event.requestContent
                    )
                }
                CommitEventType.P4 -> pipelineBuildService.externalP4Build(event.requestContent)
            }
            result = true
        } catch (ignore: Throwable) {
            logger.warn("Fail to handle the event [${event.retryTime}]", ignore)
        } finally {
            if (!result) {
                retryCommitEvent(event)
            }
            saveWebhookTriggerLog(event.commitEventType.name)
            PipelineWebhookBuildLogContext.remove()
            MDC.remove(TraceTag.BIZID)
        }
    }

    private fun retryCommitEvent(event: ICodeWebhookEvent) {
        if (event.retryTime >= 0) {
            logger.warn("Retry to handle the event [${event.retryTime}]")
            with(event) {
                CodeWebhookEventDispatcher.dispatchEvent(
                    rabbitTemplate,
                    when (event.commitEventType) {
                        CommitEventType.SVN -> SvnWebhookEvent(
                            requestContent = requestContent,
                            retryTime = retryTime - 1,
                            delayMills = DELAY_MILLS
                        )
                        CommitEventType.GIT -> {
                            event as GitWebhookEvent
                            GitWebhookEvent(
                                requestContent = requestContent,
                                retryTime = retryTime - 1,
                                delayMills = DELAY_MILLS,
                                event = event.event,
                                secret = event.secret
                            )
                        }
                        CommitEventType.GITLAB -> GitlabWebhookEvent(
                            requestContent = requestContent,
                            retryTime = retryTime - 1,
                            delayMills = DELAY_MILLS
                        )
                        CommitEventType.TGIT -> {
                            event as TGitWebhookEvent
                            TGitWebhookEvent(
                                requestContent = requestContent,
                                retryTime = retryTime - 1,
                                delayMills = DELAY_MILLS,
                                event = event.event,
                                secret = event.secret
                            )
                        }
                        CommitEventType.P4 -> {
                            event as P4WebhookEvent
                            P4WebhookEvent(
                                requestContent = requestContent,
                                retryTime = retryTime - 1,
                                delayMills = DELAY_MILLS
                            )
                        }
                    }

                )
            }
        }
    }

    fun handleGithubCommitEvent(event: GithubWebhookEvent) {
        val traceId = MDC.get(TraceTag.BIZID)
        if (traceId.isNullOrEmpty()) {
            if (!event.traceId.isNullOrEmpty()) {
                MDC.put(TraceTag.BIZID, event.traceId)
            } else {
                MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            }
        }
        logger.info("Receive Github from MQ [GITHUB|${event.githubWebhook.event}]")
        val thisGithubWebhook = event.githubWebhook
        var result = false
        PipelineWebhookBuildLogContext.initTriggerLog(
            codeType = CodeType.GITHUB.name,
            requestContent = thisGithubWebhook.body
        )
        try {
            pipelineBuildService.externalCodeGithubBuild(
                eventType = thisGithubWebhook.event,
                guid = thisGithubWebhook.guid,
                signature = thisGithubWebhook.signature,
                body = thisGithubWebhook.body
            )
            result = true
        } catch (ignore: Throwable) {
            logger.warn("Fail to handle the Github event [${event.retryTime}]", ignore)
        } finally {
            if (!result && event.retryTime >= 0) {
                logger.warn("Retry to handle the Github event [${event.retryTime}]")
                CodeWebhookEventDispatcher.dispatchGithubEvent(rabbitTemplate,
                    GithubWebhookEvent(
                        thisGithubWebhook,
                        retryTime = event.retryTime - 1,
                        delayMills = DELAY_MILLS
                    )
                )
            }
            saveWebhookTriggerLog(CodeType.GITHUB.name)
            PipelineWebhookBuildLogContext.remove()
            MDC.remove(TraceTag.BIZID)
        }
    }

    private fun saveWebhookTriggerLog(codeType: String) {
        try {
            triggerBuildLogService.saveWebhookBuildLog(PipelineWebhookBuildLogContext.get())
        } catch (ignore: Throwable) {
            logger.error("save webhook trigger log failed|codeType=$codeType", ignore)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookEventListener::class.java)
        private const val DELAY_MILLS = 3 * 1000
    }
}
