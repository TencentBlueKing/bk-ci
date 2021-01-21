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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.process.engine.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.ICodeWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.TGitWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.enum.CommitEventType
import com.tencent.devops.process.engine.service.PipelineBuildWebhookService
import com.tencent.devops.process.engine.webhook.CodeWebhookEventDispatcher
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * @ Date       ：Created in 16:34 2019-08-07
 */

@Component
class WebhookEventListener constructor(
    private val pipelineBuildService: PipelineBuildWebhookService,
    private val rabbitTemplate: RabbitTemplate
) {

    fun handleCommitEvent(event: ICodeWebhookEvent) {
        logger.info("Receive WebhookEvent from MQ [${event.commitEventType}|${event.requestContent}]")
        var result = false
        try {
            when (event.commitEventType) {
                CommitEventType.SVN -> pipelineBuildService.externalCodeSvnBuild(event.requestContent)
                CommitEventType.GIT -> pipelineBuildService.externalCodeGitBuild(CodeGitWebHookTriggerElement.classType, event.requestContent)
                CommitEventType.GITLAB -> pipelineBuildService.externalGitlabBuild(event.requestContent)
                CommitEventType.TGIT -> pipelineBuildService.externalCodeGitBuild(CodeTGitWebHookTriggerElement.classType, event.requestContent)
            }
            result = true
        } catch (t: Throwable) {
            logger.warn("Fail to handle the event [${event.retryTime}]", t)
        } finally {
            if (!result) {
                if (event.retryTime >= 0) {
                    logger.warn("Retry to handle the event [${event.retryTime}]")
                    with(event) {
                        CodeWebhookEventDispatcher.dispatchEvent(rabbitTemplate,
                            when (event.commitEventType) {
                                CommitEventType.SVN -> SvnWebhookEvent(
                                    requestContent,
                                    retryTime = retryTime - 1,
                                    delayMills = 3 * 1000
                                )
                                CommitEventType.GIT -> GitWebhookEvent(
                                    requestContent,
                                    retryTime = retryTime - 1,
                                    delayMills = 3 * 1000
                                )
                                CommitEventType.GITLAB -> GitlabWebhookEvent(
                                    requestContent,
                                    retryTime = retryTime - 1,
                                    delayMills = 3 * 1000
                                )
                                CommitEventType.TGIT -> TGitWebhookEvent(
                                    requestContent,
                                    retryTime = retryTime - 1,
                                    delayMills = 3 * 1000
                                )
                            }

                        )
                    }
                }
            }
        }
    }

    fun handleGithubCommitEvent(event: GithubWebhookEvent) {
        logger.info("Receive Github from MQ [GITHUB|${event.githubWebhook.event}]")
        val thisGithubWebhook = event.githubWebhook
        var result = false
        try {
            pipelineBuildService.externalCodeGithubBuild(
                thisGithubWebhook.event,
                thisGithubWebhook.guid,
                thisGithubWebhook.signature,
                thisGithubWebhook.body
            )
            result = true
        } catch (t: Throwable) {
            logger.warn("Fail to handle the Github event [${event.retryTime}]", t)
        } finally {
            if (!result) {
                if (event.retryTime >= 0) {
                    logger.warn("Retry to handle the Github event [${event.retryTime}]")
                    with(event) {
                        CodeWebhookEventDispatcher.dispatchGithubEvent(rabbitTemplate,
                            GithubWebhookEvent(
                                thisGithubWebhook,
                                retryTime = retryTime - 1,
                                delayMills = 3 * 1000
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookEventListener::class.java)
    }
}