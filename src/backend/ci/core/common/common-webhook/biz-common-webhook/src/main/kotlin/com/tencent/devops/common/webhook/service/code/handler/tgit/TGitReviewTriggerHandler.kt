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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_RESTRICT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitReviewTriggerHandler(
    private val eventCacheService: EventCacheService
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

    @SuppressWarnings("ComplexMethod", "ComplexCondition")
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
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWERS] =
            event.reviewers.joinToString(",") { it.reviewer.username }
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS] = approvingReviews.joinToString(",")
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS] = approvedReviews.joinToString(",")
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_STATE] = event.state
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_OWNER] = event.author.username
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_ID] = event.id
        startParams[BK_REPO_GIT_WEBHOOK_REVIEW_IID] = event.iid
        startParams[PIPELINE_GIT_EVENT_URL] = "${event.repository.homepage}/reviews/${event.iid}"
        if (event.reviewableType == "merge_request" &&
            event.reviewableId != null &&
            projectId != null &&
            repository != null
        ) {
            // MR提交人
            val mrInfo = eventCacheService.getMergeRequestInfo(projectId, event.reviewableId, repository)
            val reviewInfo =
                eventCacheService.getMergeRequestReviewersInfo(projectId, event.reviewableId, repository)

            startParams.putAll(
                WebhookUtils.mrStartParam(
                    mrInfo = mrInfo,
                    reviewInfo = reviewInfo,
                    mrRequestId = event.reviewableId!!,
                    homepage = event.repository.homepage
                )
            )
        }

        // 兼容stream变量
        startParams[PIPELINE_GIT_EVENT] = GitReviewEvent.classType
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.git_http_url
        if (projectId != null && repository != null) {
            val (defaultBranch, commitInfo) =
                eventCacheService.getDefaultBranchLatestCommitInfo(projectId = projectId, repo = repository)
            startParams[PIPELINE_GIT_REF] = defaultBranch ?: ""
            startParams[CI_BRANCH] = defaultBranch ?: ""

            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = commitInfo?.author_name ?: ""
            startParams[PIPELINE_GIT_SHA] = commitInfo?.id ?: ""
            startParams[PIPELINE_GIT_SHA_SHORT] = commitInfo?.short_id ?: ""
        }

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
            val crStateFilter = ContainsFilter(
                pipelineId = pipelineId,
                filterName = "crState",
                triggerOn = event.state,
                included = WebhookUtils.convert(includeCrState)
            )
            val crTypeFilter = ContainsFilter(
                pipelineId = pipelineId,
                filterName = "crType",
                triggerOn = event.reviewableType ?: "",
                included = WebhookUtils.convert(includeCrTypes)
            )
            return listOf(urlFilter, eventTypeFilter, crStateFilter, crTypeFilter)
        }
    }
}
