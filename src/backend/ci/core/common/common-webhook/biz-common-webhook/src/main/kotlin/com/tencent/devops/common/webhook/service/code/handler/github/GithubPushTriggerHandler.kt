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

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.code.github.GithubPushOperationKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.DELETE_EVENT
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitCommitAuthor
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubPushTriggerHandler : GitHookTriggerHandler<GithubPushEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubPushTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GithubPushEvent> {
        return GithubPushEvent::class.java
    }

    override fun getUrl(event: GithubPushEvent): String {
        return event.repository.sshUrl
    }

    override fun getUsername(event: GithubPushEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubPushEvent): String {
        return event.headCommit?.id ?: ""
    }

    override fun getRepoName(event: GithubPushEvent): String {
        return GitUtils.getProjectName(event.repository.sshUrl)
    }

    override fun getBranchName(event: GithubPushEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.PUSH
    }

    override fun getMessage(event: GithubPushEvent): String? {
        return event.headCommit?.message ?: ""
    }

    override fun preMatch(event: GithubPushEvent): ScmWebhookMatcher.MatchResult {
        if (event.commits.isEmpty()) {
            logger.info("Github web hook no commit")
            return ScmWebhookMatcher.MatchResult(false)
        }
        return ScmWebhookMatcher.MatchResult(true)
    }

    override fun getEventFilters(
        event: GithubPushEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        return emptyList()
    }

    override fun retrieveParams(
        event: GithubPushEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = event.sender.login
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = getBranchName(event)
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = event.before
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = event.after
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = event.commits.size
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND] = ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = GithubPushOperationKind.getOperationKind(event).value
        startParams.putAll(
            WebhookUtils.genCommitsParam(
                commits = event.commits.map {
                    GitCommit(
                        id = it.id,
                        message = it.message,
                        timestamp = it.timestamp,
                        author = GitCommitAuthor(name = it.author.name, email = it.author.email),
                        modified = it.modified,
                        added = it.added,
                        removed = it.removed
                    )
                }
            )
        )
        // 兼容stream变量
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.url
        startParams[PIPELINE_GIT_REF] = event.ref
        startParams[CI_BRANCH] = getBranchName(event)
        startParams[PIPELINE_GIT_EVENT] = if (event.deleted) {
            DELETE_EVENT
        } else {
            GitPushEvent.classType
        }
        startParams[PIPELINE_GIT_COMMIT_AUTHOR] =
            event.commits.firstOrNull { it.id == event.after }?.author?.name ?: ""
        startParams[PIPELINE_GIT_BEFORE_SHA] = event.before
        startParams[PIPELINE_GIT_BEFORE_SHA_SHORT] = GitUtils.getShortSha(event.before)
        startParams[PIPELINE_GIT_ACTION] = when {
            event.created && event.commits.isNotEmpty() -> TGitPushActionType.NEW_BRANCH_AND_PUSH_FILE.value
            event.created -> TGitPushActionType.NEW_BRANCH.value
            else -> TGitPushActionType.PUSH_FILE.value
        }
        startParams[PIPELINE_GIT_EVENT_URL] = "${event.repository.url}/commit/${event.commits.firstOrNull()?.id}"

        return startParams
    }
}
