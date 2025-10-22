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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.trigger.scm.WebhookGrayService
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceScmWebhookResourceImpl @Autowired constructor(
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val streamBridge: StreamBridge,
    private val pipelineWebhookService: PipelineWebhookService,
    private val webhookGrayService: WebhookGrayService
) : ServiceScmWebhookResource {
    override fun webHookCodeGithubCommit(webhook: GithubWebhook): Result<Boolean> {
        return Result(
            CodeWebhookEventDispatcher.dispatchGithubEvent(
                streamBridge = streamBridge,
                event = GithubWebhookEvent(githubWebhook = webhook)
            )
        )
    }

    override fun webhookCommit(projectId: String, webhookCommit: WebhookCommit): Result<String> {
        return Result(
            pipelineBuildWebhookService.webhookCommitTriggerPipelineBuild(projectId, webhookCommit)?.id ?: ""
        )
    }

    override fun webhookCommitNew(projectId: String, webhookCommit: WebhookCommit): Result<BuildId?> {
        return Result(
            pipelineBuildWebhookService.webhookCommitTriggerPipelineBuild(projectId, webhookCommit)
        )
    }

    override fun listScmWebhook(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<PipelineWebhook>> {
        return Result(
            pipelineWebhookService.listWebhook(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun addGrayRepoWhite(scmCode: String, pac: Boolean, serverRepoNames: List<String>): Result<Boolean> {
        webhookGrayService.addGrayRepoWhite(
            scmCode = scmCode,
            pac = pac,
            serverRepoNames = serverRepoNames
        )
        return Result(true)
    }
}
