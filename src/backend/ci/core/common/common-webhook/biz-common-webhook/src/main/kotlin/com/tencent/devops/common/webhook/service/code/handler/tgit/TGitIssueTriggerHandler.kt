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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ACTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_URL
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
class TGitIssueTriggerHandler(
    private val eventCacheService: EventCacheService
) : GitHookTriggerHandler<GitIssueEvent> {

    override fun eventClass(): Class<GitIssueEvent> {
        return GitIssueEvent::class.java
    }

    override fun getUrl(event: GitIssueEvent): String {
        return event.repository.url
    }

    override fun getUsername(event: GitIssueEvent): String {
        return event.user.username
    }

    override fun getRevision(event: GitIssueEvent): String {
        return ""
    }

    override fun getRepoName(event: GitIssueEvent): String {
        return GitUtils.getProjectName(event.repository.url)
    }

    override fun getBranchName(event: GitIssueEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.ISSUES
    }

    override fun getWebhookFilters(
        event: GitIssueEvent,
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
            val actionFilter = ContainsFilter(
                pipelineId = pipelineId,
                filterName = "issueAction",
                triggerOn = event.objectAttributes.action ?: "",
                included = WebhookUtils.convert(includeIssueAction),
                failedReason = I18Variable(
                    code = WebhookI18nConstants.ISSUES_ACTION_NOT_MATCH,
                    params = listOf()
                ).toJsonStr()
            )
            return listOf(urlFilter, eventTypeFilter, actionFilter)
        }
    }

    override fun getMessage(event: GitIssueEvent): String? {
        return event.objectAttributes.title
    }

    override fun getEventDesc(event: GitIssueEvent): String {
        return I18Variable(
            code = getI18Code(event),
            params = listOf(
                "${event.objectAttributes.url}",
                event.objectAttributes.iid,
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: GitIssueEvent): String {
        return event.objectAttributes.projectId.toString()
    }

    override fun getAction(event: GitIssueEvent): String? {
        return event.objectAttributes.action
    }

    override fun getEventFilters(
        event: GitIssueEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        val actionFilter = ContainsFilter(
            pipelineId = pipelineId,
            filterName = "issueAction",
            triggerOn = event.objectAttributes.action ?: "",
            included = WebhookUtils.convert(webHookParams.includeIssueAction),
            failedReason = I18Variable(
                code = WebhookI18nConstants.ISSUES_ACTION_NOT_MATCH,
                params = listOf(
                    "${event.objectAttributes.url}",
                    event.objectAttributes.iid,
                    getUsername(event)
                )
            ).toJsonStr()
        )
        return listOf(actionFilter)
    }

    @Suppress("ComplexMethod")
    override fun retrieveParams(event: GitIssueEvent, projectId: String?, repository: Repository?): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(event.objectAttributes) {
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_TITLE] = title
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_IID] = iid
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION] = description ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_STATE] = state
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_OWNER] = event.user.username
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_URL] = url ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID] = milestoneId ?: 0L
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ACTION] = action ?: ""
            // ci.commit_msg 取标题，跟stream同步
            startParams[PIPELINE_GIT_COMMIT_MESSAGE] = title
        }

        // 兼容stream变量
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.url
        startParams[PIPELINE_GIT_EVENT] = GitIssueEvent.classType
        if (projectId != null && repository != null) {
            val (defaultBranch, commitInfo) =
                eventCacheService.getDefaultBranchLatestCommitInfo(projectId = projectId, repo = repository)
            startParams[PIPELINE_GIT_REF] = defaultBranch ?: ""
            startParams[CI_BRANCH] = defaultBranch ?: ""
            startParams[PIPELINE_WEBHOOK_BRANCH] = defaultBranch ?: ""

            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = commitInfo?.author_name ?: ""
            startParams[PIPELINE_GIT_SHA] = commitInfo?.id ?: ""
            startParams[PIPELINE_GIT_SHA_SHORT] = commitInfo?.short_id ?: ""
        }
        startParams[PIPELINE_GIT_EVENT_URL] = event.objectAttributes.url ?: ""
        startParams[PIPELINE_GIT_ACTION] = event.objectAttributes.action ?: ""
        return startParams
    }

    private fun getI18Code(event: GitIssueEvent) = when (getAction(event)) {
        GitIssueEvent.ACTION_CREATED -> WebhookI18nConstants.TGIT_ISSUE_CREATED_EVENT_DESC
        GitIssueEvent.ACTION_UPDATED -> WebhookI18nConstants.TGIT_ISSUE_UPDATED_EVENT_DESC
        GitIssueEvent.ACTION_CLOSED -> WebhookI18nConstants.TGIT_ISSUE_CLOSED_EVENT_DESC
        GitIssueEvent.ACTION_REOPENED -> WebhookI18nConstants.TGIT_ISSUE_REOPENED_EVENT_DESC
        else -> ""
    }
}
