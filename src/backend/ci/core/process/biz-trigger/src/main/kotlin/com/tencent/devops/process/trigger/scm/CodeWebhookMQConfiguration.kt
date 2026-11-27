package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.trigger.event.GitWebhookEvent
import com.tencent.devops.process.trigger.event.GithubWebhookEvent
import com.tencent.devops.process.trigger.event.GitlabWebhookEvent
import com.tencent.devops.process.trigger.event.P4WebhookEvent
import com.tencent.devops.process.trigger.event.ReplayWebhookEvent
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.process.trigger.event.ScmWebhookTriggerEvent
import com.tencent.devops.process.trigger.event.SvnWebhookEvent
import com.tencent.devops.process.trigger.event.TGitWebhookEvent
import com.tencent.devops.process.trigger.scm.listener.CodeWebhookEventListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 代码库 webhook 事件消费
 *
 * process 与 trigger 共用同一消费组，trigger 独立部署时配置 trigger.standalone.scm-consumer.enabled=false，
 * 避免 trigger 消费后再通过 http 调用 process
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "trigger.standalone.scm-consumer", name = ["enabled"], matchIfMissing = true)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("TooManyFunctions")
class CodeWebhookMQConfiguration {
    @Bean
    fun codeWebhookEventListener(
        @Autowired streamBridge: StreamBridge,
        @Autowired webhookRequestService: WebhookRequestService,
        @Autowired scmWebhookReplayService: ScmWebhookReplayService
    ) = CodeWebhookEventListener(
        streamBridge = streamBridge,
        webhookRequestService = webhookRequestService,
        scmWebhookReplayService = scmWebhookReplayService
    )

    // 各类Commit事件监听
    @EventConsumer(groupName = "process\${service-suffix}-githubWebhookConsumer")
    fun githubWebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<GithubWebhookEvent> { codeWebhookEventListener.handleGithubCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-gitlabWebhookConsumer")
    fun gitlabWebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<GitlabWebhookEvent> { codeWebhookEventListener.handleCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-gitWebhookConsumer")
    fun gitWebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<GitWebhookEvent> { codeWebhookEventListener.handleCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-p4WebhookConsumer")
    fun p4WebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<P4WebhookEvent> { codeWebhookEventListener.handleCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-svnWebhookConsumer")
    fun svnWebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<SvnWebhookEvent> { codeWebhookEventListener.handleCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-tgitWebhookConsumer")
    fun tgitWebhookConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<TGitWebhookEvent> { codeWebhookEventListener.handleCommitEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-replayEventConsumer")
    fun replayEventConsumer(
        @Autowired codeWebhookEventListener: CodeWebhookEventListener
    ) = ScsConsumerBuilder.build<ReplayWebhookEvent> { codeWebhookEventListener.handleReplayEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-scmWebhookRequestEventConsumer")
    fun scmWebhookRequestEventConsumer(
        @Autowired webhookManager: WebhookManager
    ) = ScsConsumerBuilder.build<ScmWebhookRequestEvent> { webhookManager.handleRequestEvent(it) }

    @EventConsumer(groupName = "process\${service-suffix}-scmWebhookTriggerEventConsumer")
    fun scmWebhookTriggerEventConsumer(
        @Autowired webhookTriggerBuildService: ScmWebhookTriggerBuildService
    ) = ScsConsumerBuilder.build<ScmWebhookTriggerEvent> { webhookTriggerBuildService.trigger(it) }
}
