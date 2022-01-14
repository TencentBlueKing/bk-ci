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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.SkipCiFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.common.webhook.util.WebhookUtils.convert
import com.tencent.devops.process.engine.service.code.filter.CommitMessageFilter
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitPushTriggerHandler : GitHookTriggerHandler<GitPushEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitPushTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GitPushEvent> {
        return GitPushEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.PUSH
    }

    override fun getUrl(event: GitPushEvent): String {
        return event.repository.git_http_url
    }

    override fun getUsername(event: GitPushEvent): String {
        return event.user_name
    }

    override fun getRevision(event: GitPushEvent): String {
        return event.checkout_sha ?: ""
    }

    override fun getRepoName(event: GitPushEvent): String {
        return GitUtils.getProjectName(event.repository.git_ssh_url)
    }

    override fun getBranchName(event: GitPushEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getMessage(event: GitPushEvent): String {
        return if (event.commits.isNullOrEmpty()) {
            ""
        } else {
            event.commits!![0].message
        }
    }

    override fun preMatch(event: GitPushEvent): ScmWebhookMatcher.MatchResult {
        val isMatch = when {
            event.total_commits_count <= 0 -> {
                logger.info("Git web hook no commit(${event.total_commits_count})")
                false
            }
            GitUtils.isPrePushBranch(event.ref) -> {
                logger.info("Git web hook is pre-push event|branchName=${event.ref}")
                false
            }
            else ->
                true
        }
        return ScmWebhookMatcher.MatchResult(isMatch)
    }

    override fun getEventFilters(
        event: GitPushEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val skipCiFilter = SkipCiFilter(
                pipelineId = pipelineId,
                triggerOnMessage = event.commits?.get(0)?.message ?: ""
            )
            val commits = event.commits
            val eventPaths = mutableSetOf<String>()
            commits?.forEach { commit ->
                eventPaths.addAll(commit.added ?: listOf())
                eventPaths.addAll(commit.removed ?: listOf())
                eventPaths.addAll(commit.modified ?: listOf())
            }
            val commitMessageFilter = CommitMessageFilter(
                includeCommitMsg,
                excludeCommitMsg,
                commits?.first()?.message ?: "",
                pipelineId
            )
            val pathFilter = PathFilterFactory.newPathFilter(
                PathFilterConfig(
                    pathFilterType = pathFilterType,
                    pipelineId = pipelineId,
                    triggerOnPath = eventPaths.toList(),
                    includedPaths = convert(includePaths),
                    excludedPaths = convert(excludePaths)
                )
            )
            return listOf(skipCiFilter, pathFilter, commitMessageFilter)
        }
    }

    override fun retrieveParams(
        event: GitPushEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = event.user_name
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = event.before
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = event.after
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = event.total_commits_count
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND] = event.action_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = event.operation_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = getBranchName(event)
        startParams.putAll(WebhookUtils.genCommitsParam(commits = event.commits ?: emptyList()))
        return startParams
    }
}
