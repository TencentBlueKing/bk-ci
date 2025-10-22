/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.webhook

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.process.trigger.scm.WebhookManager
import com.tencent.devops.process.webhook.listener.WebhookEventListener
import com.tencent.devops.process.webhook.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.P4WebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.TGitWebhookEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * @ Date       ：Created in 16:50 2019-08-07
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("TooManyFunctions")
class WebhookMQConfiguration @Autowired constructor() {
    @Bean
    fun webhookEventListener(
        @Autowired streamBridge: StreamBridge,
        @Autowired webhookRequestService: WebhookRequestService
    ) = WebhookEventListener(
        streamBridge = streamBridge,
        webhookRequestService = webhookRequestService
    )

    // 各类Commit事件监听
    @EventConsumer
    fun githubWebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<GithubWebhookEvent> { webhookEventListener.handleGithubCommitEvent(it) }

    @EventConsumer
    fun gitlabWebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<GitlabWebhookEvent> { webhookEventListener.handleCommitEvent(it) }

    @EventConsumer
    fun gitWebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<GitWebhookEvent> { webhookEventListener.handleCommitEvent(it) }

    @EventConsumer
    fun p4WebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<P4WebhookEvent> { webhookEventListener.handleCommitEvent(it) }

    @EventConsumer
    fun svnWebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<SvnWebhookEvent> { webhookEventListener.handleCommitEvent(it) }

    @EventConsumer
    fun tgitWebhookConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<TGitWebhookEvent> { webhookEventListener.handleCommitEvent(it) }

    @EventConsumer
    fun replayEventConsumer(
        @Autowired webhookEventListener: WebhookEventListener
    ) = ScsConsumerBuilder.build<ReplayWebhookEvent> { webhookEventListener.handleReplayEvent(it) }

    @EventConsumer
    fun scmWebhookRequestEventConsumer(
        @Autowired webhookManager: WebhookManager
    ) = ScsConsumerBuilder.build<ScmWebhookRequestEvent> { webhookManager.handleRequestEvent(it) }
}
