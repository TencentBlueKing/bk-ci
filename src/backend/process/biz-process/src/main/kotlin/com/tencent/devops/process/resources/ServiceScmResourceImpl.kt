package com.tencent.devops.process.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.ServiceScmResource
import com.tencent.devops.process.engine.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.engine.service.PipelineBuildWebhookService
import com.tencent.devops.process.engine.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.code.github.GithubWebhook
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceScmResourceImpl @Autowired constructor(
    private val pipelineBuildService: PipelineBuildWebhookService,
    private val rabbitTemplate: RabbitTemplate
) : ServiceScmResource {
    override fun webHookCodeGithubCommit(webhook: GithubWebhook): Result<Boolean> {
        return Result(CodeWebhookEventDispatcher.dispatchGithubEvent(rabbitTemplate, GithubWebhookEvent(githubWebhook = webhook)))
    }

    override fun webhookCommit(projectId: String, webhookCommit: WebhookCommit): Result<String> {
        return Result(pipelineBuildService.webhookCommitTriggerPipelineBuild(projectId, webhookCommit))
    }
}