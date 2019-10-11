package com.tencent.devops.process.engine.listener.run

import com.tencent.devops.process.engine.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.ICodeWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.enum.CommitEventType
import com.tencent.devops.process.engine.service.PipelineBuildWebhookService
import com.tencent.devops.process.engine.webhook.CodeWebhookEventDispatcher
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * @ Author     ：Royal Huang
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
                CommitEventType.GIT -> pipelineBuildService.externalCodeGitBuild("", event.requestContent)
                CommitEventType.GITLAB -> pipelineBuildService.externalGitlabBuild(event.requestContent)
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
                            }

                        )
                    }
                }
            }
        }
    }

    fun handleGithubCommitEvent(event: GithubWebhookEvent) {
        logger.info("Receive Github from MQ [GIHUB|${event.githubWebhook.event}]")
        var thisGithubWebhook = event.githubWebhook
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