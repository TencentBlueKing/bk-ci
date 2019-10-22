package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.engine.webhook.CodeWebhookEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalScmResourceImpl @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) : ExternalScmResource {

    override fun webHookCodeSvnCommit(event: String) =
            Result(CodeWebhookEventDispatcher.dispatchEvent(rabbitTemplate, SvnWebhookEvent(requestContent = event)))

    override fun webHookCodeGitCommit(token: String, event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(rabbitTemplate, GitWebhookEvent(requestContent = event)))

    override fun webHookGitlabCommit(event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(rabbitTemplate, GitlabWebhookEvent(requestContent = event)))
}
