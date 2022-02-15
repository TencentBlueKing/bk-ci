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
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequest
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.GITHUB_PR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@SuppressWarnings("TooManyFunctions")
class GithubPrTriggerHandler : GitHookTriggerHandler<GithubPullRequestEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubPrTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GithubPullRequestEvent> {
        return GithubPullRequestEvent::class.java
    }

    override fun getUrl(event: GithubPullRequestEvent): String {
        return event.repository.clone_url
    }

    override fun getUsername(event: GithubPullRequestEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubPullRequestEvent): String {
        return event.pull_request.head.sha
    }

    override fun getRepoName(event: GithubPullRequestEvent): String {
        return GitUtils.getProjectName(event.repository.ssh_url)
    }

    override fun getBranchName(event: GithubPullRequestEvent): String {
        return event.pull_request.base.ref
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.PULL_REQUEST
    }

    override fun getMessage(event: GithubPullRequestEvent): String? {
        return ""
    }

    override fun getEnv(event: GithubPullRequestEvent): Map<String, Any> {
        return mapOf(GITHUB_PR_NUMBER to event.number)
    }

    override fun getHookSourceUrl(event: GithubPullRequestEvent): String? {
        return event.pull_request.head.repo.clone_url
    }

    override fun getHookTargetUrl(event: GithubPullRequestEvent): String? {
        return event.pull_request.base.repo.clone_url
    }

    override fun preMatch(event: GithubPullRequestEvent): ScmWebhookMatcher.MatchResult {
        if (!(event.action == "opened" || event.action == "reopened" || event.action == "synchronize")) {
            logger.info("Github pull request no open or update")
            return ScmWebhookMatcher.MatchResult(false)
        }
        return ScmWebhookMatcher.MatchResult(true)
    }

    override fun getEventFilters(
        event: GithubPullRequestEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        return emptyList()
    }

    override fun retrieveParams(
        event: GithubPullRequestEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = event.sender.login
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = event.number
        pullRequestStartParam(pullRequest = event.pull_request, startParams = startParams)
        return startParams
    }

    private fun pullRequestStartParam(pullRequest: GithubPullRequest, startParams: MutableMap<String, Any>) {
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = pullRequest.base.repo.clone_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = pullRequest.head.repo.clone_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = pullRequest.base.ref
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = pullRequest.head.ref
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = pullRequest.created_at ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = pullRequest.update_at ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = pullRequest.id
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = pullRequest.comments_url ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = pullRequest.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] =
            pullRequest.assignees.joinToString(",") { it.login ?: "" }
        startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = pullRequest.url
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
            pullRequest.requested_reviewers.joinToString(",") { it.login ?: "" }
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = pullRequest.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] =
            pullRequest.milestone?.due_on ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] =
            pullRequest.labels.joinToString(",") { it.name }
        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = pullRequest.head.ref
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = pullRequest.base.ref
    }
}
