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

package com.tencent.devops.plugin.init

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.plugin.listener.CodeWebhookListener
import com.tencent.devops.plugin.listener.GitHubPullRequestListener
import com.tencent.devops.plugin.listener.TGitCommitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * 流水线监控配置
 */
@Configuration
@Suppress("TooManyFunctions")
class CodeWebhookListenerConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "plugin-service"
    }

    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun codeWebhookFinishListener(
        @Autowired listener: CodeWebhookListener
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            listener.onBuildFinished(event.payload)
        }
    }

    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT, STREAM_CONSUMER_GROUP)
    fun codeWebhookQueueListener(
        @Autowired listener: CodeWebhookListener
    ): Consumer<Message<PipelineBuildQueueBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildQueueBroadCastEvent> ->
            listener.onBuildQueue(event.payload)
        }
    }

    /**
     * gitcommit队列--- 并发小
     */
    @EventConsumer(StreamBinding.QUEUE_GIT_COMMIT_CHECK, STREAM_CONSUMER_GROUP)
    fun gitCommitCheckListener(
        @Autowired listener: TGitCommitListener
    ): Consumer<Message<GitCommitCheckEvent>> {
        return Consumer { event: Message<GitCommitCheckEvent> ->
            listener.execute(event.payload)
        }
    }

    /**
     * github pr队列--- 并发小
     */
    @EventConsumer(StreamBinding.QUEUE_GITHUB_PR, STREAM_CONSUMER_GROUP)
    fun githubPrQueueListener(
        @Autowired listener: GitHubPullRequestListener
    ): Consumer<Message<GithubPrEvent>> {
        return Consumer { event: Message<GithubPrEvent> ->
            listener.execute(event.payload)
        }
    }
}
