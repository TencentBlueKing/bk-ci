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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.CodeReviewStateFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_REVIEW_RESTRICT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_SOURCE_PROJECT_ID
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.scm.pojo.PIPELINE_WEBHOOK_TARGET_PROJECT_ID
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitReviewTriggerHandler(
    private val gitScmService: GitScmService
) : CodeWebhookTriggerHandler<GitReviewEvent> {
    override fun eventClass(): Class<GitReviewEvent> {
        return GitReviewEvent::class.java
    }

    override fun getUrl(event: GitReviewEvent): String {
        return event.repository.git_http_url
    }

    override fun getUsername(event: GitReviewEvent): String {
        return event.author.username
    }

    override fun getRevision(event: GitReviewEvent): String {
        return ""
    }

    override fun getRepoName(event: GitReviewEvent): String {
        return GitUtils.getProjectName(event.repository.git_ssh_url)
    }

    override fun getBranchName(event: GitReviewEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.REVIEW
    }

    override fun getMessage(event: GitReviewEvent): String? {
        return ""
    }

    override fun retrieveParams(event: GitReviewEvent, projectId: String?, repository: Repository?): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_ID] = event.reviewableId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE] = event.reviewableType ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_RESTRICT_TYPE] = event.restrictType ?: ""
        val approvingReviews = mutableListOf<String>()
        val approvedReviews = mutableListOf<String>()
        event.reviewers.forEach { reviewer ->
            when (reviewer.state) {
                "approving" -> approvingReviews.add(reviewer.reviewer.username)
                "approved" -> approvedReviews.add(reviewer.reviewer.username)
            }
        }
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS] = approvingReviews.joinToString(",")
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS] = approvedReviews.joinToString(",")
        if (event.reviewableType == "merge_request" && event.reviewableId != null) {
            startParams.putAll(
                mrStartParam(
                    mrRequestId = event.reviewableId!!,
                    projectId = projectId,
                    repository = repository
                )
            )
        }
        return startParams
    }

    @SuppressWarnings("ComplexMethod")
    private fun mrStartParam(
        mrRequestId: Long,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        if (projectId == null || repository == null) {
            return emptyMap()
        }
        val startParams = mutableMapOf<String, Any>()
        // MR提交人
        val mrInfo = gitScmService.getMergeRequestInfo(projectId, mrRequestId, repository)
        val reviewers = gitScmService.getMergeRequestReviewersInfo(projectId, mrRequestId, repository)?.reviewers

        startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = mrInfo?.targetBranch ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = mrInfo?.sourceBranch ?: ""
        startParams[PIPELINE_WEBHOOK_SOURCE_PROJECT_ID] = mrInfo?.sourceProjectId ?: ""
        startParams[PIPELINE_WEBHOOK_TARGET_PROJECT_ID] = mrInfo?.targetProjectId ?: ""
        startParams[PIPELINE_WEBHOOK_MR_ID] = mrRequestId
        startParams[PIPELINE_WEBHOOK_MR_COMMITTER] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = mrInfo?.author?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = mrInfo?.createTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = mrInfo?.updateTime ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.createTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] = DateTimeUtil.zoneDateToTimestamp(mrInfo?.updateTime)
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = mrInfo?.mrId ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = mrInfo?.mrNumber ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = mrInfo?.description ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = mrInfo?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = mrInfo?.assignee?.username ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] = reviewers?.joinToString(",") { it.username } ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = mrInfo?.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] = mrInfo?.milestone?.dueDate ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] = mrInfo?.labels?.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT] = mrInfo?.baseCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT] = mrInfo?.targetCommit ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT] = mrInfo?.sourceCommit ?: ""
        return startParams
    }

    override fun getWebhookFilters(
        event: GitReviewEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val urlFilter = GitUrlFilter(
                pipelineId = pipelineId,
                triggerOnUrl = getUrl(event),
                repositoryUrl = repository.url,
                includeHost = includeHost
            )
            val eventTypeFilter = EventTypeFilter(
                pipelineId = pipelineId,
                triggerOnEventType = getEventType(),
                eventType = eventType
            )
            val codeReviewStateFilter = CodeReviewStateFilter(
                pipelineId = pipelineId,
                triggerOnState = event.state,
                includedState = WebhookUtils.convert(includeCrState)
            )
            return listOf(urlFilter, eventTypeFilter, codeReviewStateFilter)
        }
    }
}
