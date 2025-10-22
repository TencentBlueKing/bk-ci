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

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubBaseInfo
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubReviewState
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubReviewTriggerHandler @Autowired constructor(
    val eventCacheService: EventCacheService
) : CodeWebhookTriggerHandler<GithubReviewEvent> {
    override fun eventClass(): Class<GithubReviewEvent> {
        return GithubReviewEvent::class.java
    }

    override fun getUrl(event: GithubReviewEvent): String {
        return with(event) {
            repository.htmlUrl ?: "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}"
        }
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
        return CodeEventType.REVIEW
    }

    override fun getMessage(event: GithubReviewEvent): String? {
        return event.pullRequest.title
    }

    override fun getExternalId(event: GithubReviewEvent): String {
        return event.repository.id.toString()
    }

    override fun getEventDesc(event: GithubReviewEvent): String {
        return I18Variable(
            code = getI18Code(event),
            params = listOf(
                buildReviewUrl(event),
                event.pullRequest.number.toString(),
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun preMatch(event: GithubReviewEvent): WebhookMatchResult {
        // Review事件仅提交操作才触发，评审通过、拒绝、要求修改
        val result = (event.action == "submitted" || event.action == "dismissed") &&
            event.review.state != GithubReviewState.COMMENTED.value
        return WebhookMatchResult(result)
    }

    override fun getWebhookFilters(
        event: GithubReviewEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            // 非[拒绝]和[要求修改]才调用详情接口，更新事件状态
            if (event.convertState() != "change_required" || event.convertState() != "change_denied") {
                eventCacheService.getPrInfo(
                    githubRepoName = event.repository.fullName,
                    pullNumber = event.pullRequest.number.toString(),
                    repo = repository,
                    projectId = projectId
                )?.run {
                    event.pullRequest.mergeable = this.mergeable
                    event.pullRequest.mergeableState = this.mergeableState
                }
            }
            logger.info("github review event[$event]")
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
                included = WebhookUtils.convert(includeCrState),
                failedReason = I18Variable(
                    code = WebhookI18nConstants.REVIEW_ACTION_NOT_MATCH,
                    params = listOf()
                ).toJsonStr()
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
            startParams[PIPELINE_GIT_EVENT_URL] = buildReviewUrl(event)
            event.pullRequest.requestedReviewers.run {
                startParams[BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS] = this.joinToString(",") {
                    it.login
                }
            }
            // 评审类型，跟CODE_GIT保持一致，CODE_GIT中为merge_request、comparison，此处写死pull_request
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE] = "pull_request"
            startParams[PIPELINE_GIT_REPO_URL] = event.repository.getRepoUrl()
            startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = event.pullRequest.head.ref
        }
        return startParams
    }

    private fun getI18Code(event: GithubReviewEvent) = when (event.review.state) {
        GithubReviewState.APPROVED.value -> WebhookI18nConstants.TGIT_REVIEW_APPROVED_EVENT_DESC
        GithubReviewState.CHANGES_REQUESTED.value -> WebhookI18nConstants.TGIT_REVIEW_CHANGE_REQUIRED_EVENT_DESC
        GithubReviewState.DISMISSED.value -> WebhookI18nConstants.TGIT_REVIEW_CHANGE_DENIED_EVENT_DESC
        else -> ""
    }

    private fun buildReviewUrl(event: GithubReviewEvent) = with(event) {
        review.htmlUrl ?: "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}/pull/${pullRequest.number}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GithubReviewTriggerHandler::class.java)
    }
}
