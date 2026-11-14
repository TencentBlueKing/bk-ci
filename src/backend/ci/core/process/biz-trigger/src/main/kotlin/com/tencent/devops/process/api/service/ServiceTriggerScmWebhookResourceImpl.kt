package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge

/**
 * 路径与 process 的 ServiceScmWebhookResourceImpl 相同，同进程加载会导致 Jersey 资源冲突，
 * 只能在 trigger 独立部署（trigger.standalone.enabled=true）时加载
 */
@RestResource
@ConditionalOnProperty(prefix = "trigger.standalone", name = ["enabled"], havingValue = "true")
class ServiceTriggerScmWebhookResourceImpl @Autowired constructor(
    private val streamBridge: StreamBridge
) : ServiceTriggerScmWebhookResource {

    override fun webHookCodeGithubCommit(webhook: GithubWebhook): Result<Boolean> {
        return Result(
            CodeWebhookEventDispatcher.dispatchGithubEvent(
                streamBridge = streamBridge,
                event = GithubWebhookEvent(githubWebhook = webhook)
            )
        )
    }
}
