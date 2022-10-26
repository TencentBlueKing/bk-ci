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

package com.tencent.devops.process.webhook

import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.engine.service.PipelineWebhookBuildLogService
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.webhook.listener.WebhookEventListener
import com.tencent.devops.process.webhook.pojo.event.commit.GitWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.GitlabWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.P4WebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.TGitWebhookEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * @ Date       ：Created in 16:50 2019-08-07
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("TooManyFunctions")
class WebhookMQConfiguration @Autowired constructor() {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "process-service"
    }

    @Bean
    fun webhookEventListener(
        @Autowired pipelineBuildService: PipelineBuildWebhookService,
        @Autowired streamBridge: StreamBridge,
        @Autowired triggerBuildLogService: PipelineWebhookBuildLogService
    ) = WebhookEventListener(
        pipelineBuildService = pipelineBuildService,
        streamBridge = streamBridge,
        triggerBuildLogService = triggerBuildLogService
    )

    // 各类Commit事件监听
    @StreamEventConsumer(StreamBinding.QUEUE_GITHUB_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun githubWebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<GithubWebhookEvent>> {
        return Consumer { event: Message<GithubWebhookEvent> ->
            webhookEventListener.handleGithubCommitEvent(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.QUEUE_GITLAB_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun gitlabWebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<GitlabWebhookEvent>> {
        return Consumer { event: Message<GitlabWebhookEvent> ->
            webhookEventListener.handleCommitEvent(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.QUEUE_GIT_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun gitWebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<GitWebhookEvent>> {
        return Consumer { event: Message<GitWebhookEvent> ->
            webhookEventListener.handleCommitEvent(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.QUEUE_P4_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun p4WebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<P4WebhookEvent>> {
        return Consumer { event: Message<P4WebhookEvent> ->
            webhookEventListener.handleCommitEvent(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.QUEUE_SVN_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun svnWebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<SvnWebhookEvent>> {
        return Consumer { event: Message<SvnWebhookEvent> ->
            webhookEventListener.handleCommitEvent(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.QUEUE_TGIT_BUILD_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun tgitWebhookListener(
        @Autowired webhookEventListener: WebhookEventListener
    ): Consumer<Message<TGitWebhookEvent>> {
        return Consumer { event: Message<TGitWebhookEvent> ->
            webhookEventListener.handleCommitEvent(event.payload)
        }
    }
}
