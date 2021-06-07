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

package com.tencent.devops.common.webhook.service.code.matcher

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.service.code.loader.CodeWebhookHandlerRegistrar
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory

@Suppress("ALL")
open class GitWebHookMatcher(
    val event: GitEvent
) : ScmWebhookMatcher {
    companion object {
        private val logger = LoggerFactory.getLogger(GitWebHookMatcher::class.java)
        const val MATCH_BRANCH = "matchBranch"
        const val MATCH_PATHS = "matchPaths"
    }

    private val eventHandler = CodeWebhookHandlerRegistrar.getHandler(webhookEvent = event)

    override fun preMatch(): ScmWebhookMatcher.MatchResult {
        return eventHandler.preMatch(event = event)
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): ScmWebhookMatcher.MatchResult {
        if (repository !is CodeGitRepository &&
            repository !is CodeTGitRepository
        ) {
            logger.warn("Is not code repo for git web hook for repo and pipeline: $repository, $pipelineId")
            return ScmWebhookMatcher.MatchResult(isMatch = false)
        }
        return eventHandler.isMatch(
            event = event,
            projectId = projectId,
            pipelineId = pipelineId,
            repository = repository,
            webHookParams = webHookParams
        )
    }

    override fun getUsername(): String {
        return eventHandler.getUsername(event)
    }

    override fun getRevision(): String {
       return eventHandler.getRevision(event)
    }

    override fun getEventType(): CodeEventType {
        return eventHandler.getEventType()
    }

    override fun getHookSourceUrl(): String? {
        return eventHandler.getHookSourceUrl(event)
    }

    override fun getHookTargetUrl(): String? {
        return eventHandler.getHookTargetUrl(event)
    }

    override fun getCodeType() = CodeType.GIT

    override fun getEnv(): Map<String, Any> {
        return eventHandler.getEnv(event)
    }

    override fun getRepoName(): String {
        return eventHandler.getRepoName(event)
    }

    override fun getBranchName(): String {
        return eventHandler.getBranchName(event)
    }

    override fun getMergeRequestId(): Long? {
        return eventHandler.getMergeRequestId(event)
    }

    override fun getMessage(): String? {
        return eventHandler.getMessage(event)
    }
}
