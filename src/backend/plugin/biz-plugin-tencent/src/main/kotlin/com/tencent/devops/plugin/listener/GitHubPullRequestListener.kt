package com.tencent.devops.plugin.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.plugin.service.git.CodeWebhookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GitHubPullRequestListener @Autowired constructor(
    private val codeWebhookService: CodeWebhookService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<GithubPrEvent>(pipelineEventDispatcher) {

    override fun run(event: GithubPrEvent) {
        codeWebhookService.consumeGitHubPrEvent(event)
    }
}
