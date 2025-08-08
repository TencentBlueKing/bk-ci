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

package com.tencent.devops.plugin.init

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.plugin.api.pojo.GithubPrEvent
import com.tencent.devops.plugin.service.git.CodeWebhookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 流水线监控配置
 */
@Configuration
@Suppress("TooManyFunctions")
class CodeWebhookListenerConfiguration {
    @EventConsumer
    fun codeWebhookFinishConsumer(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<PipelineBuildFinishBroadCastEvent> {
        codeWebhookService.onBuildFinished(it)
    }

    @EventConsumer
    fun codeWebhookQueueConsumer(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<PipelineBuildQueueBroadCastEvent> { codeWebhookService.onBuildQueue(it) }

    /**
     * gitcommit队列--- 并发小
     */
    @EventConsumer
    fun gitCommitCheckConsumer(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<GitCommitCheckEvent> { codeWebhookService.consumeGitCommitCheckEvent(it) }

    /**
     * github pr队列--- 并发小
     */
    @EventConsumer
    fun githubPrQueueConsumer(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<GithubPrEvent> { codeWebhookService.consumeGitHubPrEvent(it) }
}
