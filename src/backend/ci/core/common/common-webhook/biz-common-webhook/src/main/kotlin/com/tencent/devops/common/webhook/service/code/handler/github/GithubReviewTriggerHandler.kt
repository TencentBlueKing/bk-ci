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
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewState
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubReviewTriggerHandler : CodeWebhookTriggerHandler<GithubReviewEvent> {
    override fun eventClass(): Class<GithubReviewEvent> {
        return GithubReviewEvent::class.java
    }

    override fun getUrl(event: GithubReviewEvent): String {
        return event.repository.htmlUrl
    }

    override fun getUsername(event: GithubReviewEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubReviewEvent): String {
        return ""
    }

    override fun getRepoName(event: GithubReviewEvent): String {
        return event.repository.fullName
    }

    override fun getBranchName(event: GithubReviewEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.ISSUES
    }

    override fun getMessage(event: GithubReviewEvent): String? {
        return event.pullRequest.title
    }

    override fun preMatch(event: GithubReviewEvent): ScmWebhookMatcher.MatchResult {
        // Review事件仅提交操作才触发，评审通过、拒绝、要求修改
        val result = event.action == "submitted" &&
            event.convertState().isNotBlank()
        return ScmWebhookMatcher.MatchResult(result)
    }

    override fun getWebhookFilters(
        event: GithubReviewEvent,
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
                triggerOn = event.convertState(),
                included = WebhookUtils.convert(includeCrState)
            )
            return listOf(urlFilter, eventTypeFilter, crStateFilter)
        }
    }

    override fun retrieveParams(
        event: GithubReviewEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        // PR 相关启动参数
        val prStartParam = WebhookUtils.prStartParam(
            pullRequest = event.pullRequest,
            homepage = event.repository.homepage
        )
        startParams.putAll(prStartParam)
        with(event.review) {
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_IID] = event.pullRequest.number
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_STATE] = state
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_OWNER] = user.login
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH] = event.pullRequest.head.ref
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH] = event.pullRequest.base.ref
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID] = event.pullRequest.head.repo.id
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID] = event.pullRequest.base.repo.id
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS] = event
            event.pullRequest.requestedReviewers.run {
                startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS] = this.joinToString(",") {
                    it.login
                }
            }
        }
        return startParams
    }
}
