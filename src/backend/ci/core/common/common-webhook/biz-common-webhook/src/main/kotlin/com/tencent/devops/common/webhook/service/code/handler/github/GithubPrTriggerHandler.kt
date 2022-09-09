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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_DESC
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_IID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_PROPOSER
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_TITLE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_TYPE
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
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
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
        return event.repository.cloneUrl
    }

    override fun getUsername(event: GithubPullRequestEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubPullRequestEvent): String {
        return event.pullRequest.head.sha
    }

    override fun getRepoName(event: GithubPullRequestEvent): String {
        return GitUtils.getProjectName(event.repository.sshUrl)
    }

    override fun getBranchName(event: GithubPullRequestEvent): String {
        return event.pullRequest.base.ref
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
        return event.pullRequest.head.repo.cloneUrl
    }

    override fun getHookTargetUrl(event: GithubPullRequestEvent): String? {
        return event.pullRequest.base.repo.cloneUrl
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
        pullRequestStartParam(event = event, startParams = startParams)

        startParams.putAll(
            WebhookUtils.prStartParam(
                pullRequest = event.pullRequest,
                homepage = event.repository.url
            )
        )
        return startParams
    }

    @Suppress("ComplexMethod")
    private fun pullRequestStartParam(event: GithubPullRequestEvent, startParams: MutableMap<String, Any>) {
        with(event) {
            startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = pullRequest.base.repo.cloneUrl
            startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = pullRequest.head.repo.cloneUrl
            startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = pullRequest.base.ref
            startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = pullRequest.head.ref
            startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = pullRequest.createdAt ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = pullRequest.updatedAt ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = pullRequest.id
            startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = pullRequest.commentsUrl ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = pullRequest.title ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] =
                pullRequest.assignees.joinToString(",") { it.login ?: "" }
            startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = pullRequest.url
            startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
                pullRequest.requestedReviewers.joinToString(",") { it.login ?: "" }
            startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = pullRequest.milestone?.title ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] =
                pullRequest.milestone?.dueOn ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] =
                pullRequest.labels.joinToString(",") { it.name }
            startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = pullRequest.head.ref
            startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = pullRequest.base.ref
            startParams[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID] = pullRequest.head.repo.id
            startParams[PIPELINE_WEBHOOK_TARGET_PROJECT_ID] = pullRequest.base.repo.id
            startParams[PIPELINE_WEBHOOK_SOURCE_REPO_NAME] = pullRequest.head.repo.fullName
            startParams[PIPELINE_WEBHOOK_TARGET_REPO_NAME] = pullRequest.base.repo.fullName
            startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT] = pullRequest.head.sha
            startParams[BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG] = ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_MERGE_TYPE] = ""
            startParams[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA] = pullRequest.mergeCommitSha ?: ""

            // 兼容stream变量
            startParams[PIPELINE_GIT_REPO_URL] = pullRequest.base.repo.url
            startParams[PIPELINE_GIT_BASE_REPO_URL] = pullRequest.head.repo.url
            startParams[PIPELINE_GIT_HEAD_REPO_URL] = pullRequest.base.repo.url
            startParams[PIPELINE_GIT_MR_URL] = pullRequest.htmlUrl ?: ""
            startParams[PIPELINE_GIT_EVENT] = GithubPullRequestEvent.classType
            startParams[PIPELINE_GIT_HEAD_REF] = pullRequest.base.ref
            startParams[PIPELINE_GIT_BASE_REF] = pullRequest.head.ref
            startParams[PIPELINE_WEBHOOK_EVENT_TYPE] = CodeEventType.PULL_REQUEST.name
            startParams[PIPELINE_WEBHOOK_SOURCE_URL] = pullRequest.head.repo.cloneUrl
            startParams[PIPELINE_WEBHOOK_TARGET_URL] = pullRequest.base.repo.cloneUrl
            startParams[PIPELINE_GIT_MR_ID] = pullRequest.id.toString()
            startParams[PIPELINE_GIT_MR_IID] = pullRequest.number.toString()
            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = pullRequest.head.user.login
            startParams[PIPELINE_GIT_MR_TITLE] = pullRequest.title
            startParams[PIPELINE_GIT_MR_DESC] = pullRequest.body ?: ""
            startParams[PIPELINE_GIT_MR_PROPOSER] = pullRequest.user.login
            startParams[PIPELINE_GIT_EVENT_URL] = pullRequest.htmlUrl ?: ""
        }
        startParams[PIPELINE_GIT_MR_ACTION] = event.action ?: ""
        startParams[PIPELINE_GIT_ACTION] = event.action ?: ""
    }
}
