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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ACTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABELS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_COLOR
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_URL
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubBaseInfo
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssuesAction
import com.tencent.devops.common.webhook.pojo.code.github.GithubIssuesEvent
import com.tencent.devops.common.webhook.pojo.code.github.getLabelChange
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class GithubIssueTriggerHandler : CodeWebhookTriggerHandler<GithubIssuesEvent> {
    override fun eventClass(): Class<GithubIssuesEvent> {
        return GithubIssuesEvent::class.java
    }

    override fun getUrl(event: GithubIssuesEvent): String {
        return with(event) {
            repository.htmlUrl ?: "${GithubBaseInfo.GITHUB_HOME_PAGE_URL}/${repository.fullName}"
        }
    }

    override fun getUsername(event: GithubIssuesEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubIssuesEvent): String {
        return ""
    }

    override fun getRepoName(event: GithubIssuesEvent): String {
        return event.repository.fullName
    }

    override fun getBranchName(event: GithubIssuesEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.ISSUES
    }

    override fun getMessage(event: GithubIssuesEvent): String? {
        return event.issue.title
    }

    override fun getExternalId(event: GithubIssuesEvent): String {
        return event.repository.id.toString()
    }

    override fun getEventDesc(event: GithubIssuesEvent): String {
        return I18Variable(
            code = getI18Code(event),
            params = listOf(
                buildIssuesUrl(event),
                event.issue.number.toString(),
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun preMatch(event: GithubIssuesEvent): WebhookMatchResult {
        return WebhookMatchResult(true)
    }

    override fun getWebhookFilters(
        event: GithubIssuesEvent,
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
                triggerOn = event.convertAction(),
                included = WebhookUtils.convert(includeIssueAction),
                failedReason = I18Variable(
                    code = WebhookI18nConstants.ISSUES_ACTION_NOT_MATCH,
                    params = listOf()
                ).toJsonStr()
            )
            val filters = mutableListOf<WebhookFilter>(urlFilter, eventTypeFilter, actionFilter)
            when (event.action) {
                GithubIssuesAction.ASSIGNED.value,
                GithubIssuesAction.UNASSIGNED.value -> filters.add(
                    UserFilter(
                        pipelineId = pipelineId,
                        triggerOnUser = event.assignee?.login ?: "",
                        includedUsers = WebhookUtils.convert(includeAssignees),
                        excludedUsers = WebhookUtils.convert(excludeAssignees),
                        includedFailedReason = I18Variable(
                            code = WebhookI18nConstants.OWNER_NOT_MATCH,
                            params = listOf(event.assignee?.login ?: "")
                        ).toJsonStr(),
                        excludedFailedReason = I18Variable(
                            code = WebhookI18nConstants.OWNER_IGNORED,
                            params = listOf(event.assignee?.login ?: "")
                        ).toJsonStr(),
                        filterName = "issueAssignee"
                    )
                )

                else -> event.getLabelChange()?.let { labelChange ->
                    filters.add(
                        labelChange.toFilter(
                            pipelineId = pipelineId,
                            filterName = "issueLabel",
                            included = WebhookUtils.convert(includeLabels),
                            excluded = WebhookUtils.convert(excludeLabels),
                            includeFailedReason = {
                                I18Variable(
                                    code = WebhookI18nConstants.BK_TRIGGER_LABEL_NOT_MATCH,
                                    params = listOf(labelChange.changedLabel?.name ?: "")
                                ).toJsonStr()
                            },
                            excludedFailedReason = { item ->
                                I18Variable(
                                    code = WebhookI18nConstants.BK_TRIGGER_LABEL_IGNORED,
                                    params = listOf(item)
                                ).toJsonStr()
                            }
                        )
                    )
                }
            }
            return filters
        }
    }

    override fun retrieveParams(
        event: GithubIssuesEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(event.issue) {
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_TITLE] = title
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_IID] = number
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION] = body ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_STATE] = state
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_OWNER] = user.login
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_URL] = htmlUrl ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID] = milestone?.id ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ACTION] = event.action
            startParams[PIPELINE_GIT_EVENT_URL] = buildIssuesUrl(event)
            startParams[PIPELINE_GIT_REPO_URL] = event.repository.getRepoUrl()
            startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = event.repository.defaultBranch
            startParams[PIPELINE_GIT_ACTION] = event.convertAction()
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE] = event.assignee?.login ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_ID] = event.assignee?.id ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEES] = JsonUtil.toJson(assignees.orEmpty(), false)
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ASSIGNEE_LOGINS] =
                assignees.orEmpty().joinToString(",") { it.login }
            startParams.putGithubLabelChange(
                labelChange = event.getLabelChange(),
                keys = GithubLabelParamKeys(
                    name = BK_REPO_GIT_WEBHOOK_ISSUE_LABEL,
                    id = BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_ID,
                    color = BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_COLOR,
                    description = BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_DESCRIPTION
                )
            )
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_LABELS] = JsonUtil.toJson(labels, false)
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_LABEL_NAMES] = labels.joinToString(",") { it.name }
        }
        return startParams
    }

    private fun getI18Code(event: GithubIssuesEvent) = when (event.action) {
        GithubIssuesAction.OPENED.value -> WebhookI18nConstants.TGIT_ISSUE_CREATED_EVENT_DESC
        GithubIssuesAction.EDITED.value -> WebhookI18nConstants.TGIT_ISSUE_UPDATED_EVENT_DESC
        GithubIssuesAction.CLOSED.value -> WebhookI18nConstants.TGIT_ISSUE_CLOSED_EVENT_DESC
        GithubIssuesAction.REOPENED.value -> WebhookI18nConstants.TGIT_ISSUE_REOPENED_EVENT_DESC
        GithubIssuesAction.ASSIGNED.value -> WebhookI18nConstants.GITHUB_ISSUE_ASSIGNED_EVENT_DESC
        GithubIssuesAction.UNASSIGNED.value -> WebhookI18nConstants.GITHUB_ISSUE_UNASSIGNED_EVENT_DESC
        GithubIssuesAction.LABELED.value -> WebhookI18nConstants.GITHUB_ISSUE_LABELED_EVENT_DESC
        GithubIssuesAction.UNLABELED.value -> WebhookI18nConstants.GITHUB_ISSUE_UNLABELED_EVENT_DESC
        else -> ""
    }

    private fun buildIssuesUrl(event: GithubIssuesEvent) = with(event) {
        issue.htmlUrl ?: "${repository.getRepoUrl()}/issues/${issue.number}"
    }
}
