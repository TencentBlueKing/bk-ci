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

package com.tencent.devops.process.service.commit.check.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.pojo.mq.commit.check.GithubCommitCheckEvent
import com.tencent.devops.process.pojo.mq.commit.check.TGitCommitCheckEvent
import com.tencent.devops.process.service.commit.check.CodeWebhookService
import com.tencent.devops.process.service.commit.check.listener.GitHubCommitCheckListener
import com.tencent.devops.process.service.commit.check.listener.TGitCommitCheckListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 流水线监控配置
 */
@Configuration
@Suppress("TooManyFunctions")
class CodeWebhookListenerConfiguration {
    @EventConsumer
    fun tGitCommitCheckListen(
        @Autowired tGitCommitCheckListener: TGitCommitCheckListener
    ) = ScsConsumerBuilder.build<TGitCommitCheckEvent> { tGitCommitCheckListener.execute(it) }

    @EventConsumer
    fun githubCommitCheckListen(
        @Autowired gitHubCommitCheckListener: GitHubCommitCheckListener
    ) = ScsConsumerBuilder.build<GithubCommitCheckEvent> { gitHubCommitCheckListener.execute(it) }

    @EventConsumer
    fun pipelineBuildQueueListen(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<PipelineBuildQueueBroadCastEvent> { codeWebhookService.onBuildQueue(it) }

    @EventConsumer
    fun pipelineBuildFinishListen(
        @Autowired codeWebhookService: CodeWebhookService
    ) = ScsConsumerBuilder.build<PipelineBuildFinishBroadCastEvent> { codeWebhookService.onBuildFinished(it) }
}
