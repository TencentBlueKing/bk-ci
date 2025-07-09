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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.enums.code.github.GithubCreateRefType
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
class GithubCreateTriggerHandler : GitHookTriggerHandler<GithubCreateEvent> {
    override fun eventClass(): Class<GithubCreateEvent> {
        return GithubCreateEvent::class.java
    }

    override fun getUrl(event: GithubCreateEvent): String {
        return event.repository.sshUrl
    }

    override fun getUsername(event: GithubCreateEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubCreateEvent): String {
        return ""
    }

    override fun getRepoName(event: GithubCreateEvent): String {
        return GitUtils.getProjectName(event.repository.sshUrl)
    }

    override fun getBranchName(event: GithubCreateEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.CREATE
    }

    override fun getMessage(event: GithubCreateEvent): String? {
        return event.ref
    }

    override fun getEventDesc(event: GithubCreateEvent): String {
        val (i18Code, linkUrl) = event.getI18nCodeAndLinkUrl()
        // 事件重放
        return I18Variable(
            code = i18Code,
            params = listOf(
                linkUrl,
                getBranchName(event),
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: GithubCreateEvent): String {
        return event.repository.id.toString()
    }

    override fun retrieveParams(
        event: GithubCreateEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME] = event.ref
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE] = event.ref_type
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME] = event.sender.login
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = getBranchName(event)
        startParams[PIPELINE_GIT_ACTION] = "create"
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.getRepoUrl()
        startParams[PIPELINE_GIT_EVENT_URL] = event.getI18nCodeAndLinkUrl().second
        return startParams
    }

    override fun getEventFilters(
        event: GithubCreateEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val userId = getUsername(event)
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = userId,
                includedUsers = WebhookUtils.convert(includeUsers),
                excludedUsers = WebhookUtils.convert(excludeUsers),
                includedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_NOT_MATCH,
                    params = listOf(userId)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_IGNORED,
                    params = listOf(userId)
                ).toJsonStr()
            )
            val triggerOnBranchName = getBranchName(event)
            val (includedFailedCode, excludedFailedCode) = if (event.createType() == GithubCreateRefType.TAG) {
                WebhookI18nConstants.TAG_NAME_NOT_MATCH to WebhookI18nConstants.TAG_NAME_IGNORED
            } else {
                WebhookI18nConstants.BRANCH_NOT_MATCH to WebhookI18nConstants.BRANCH_IGNORED
            }
            val branchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = triggerOnBranchName,
                includedBranches = WebhookUtils.convert(branchName),
                excludedBranches = WebhookUtils.convert(excludeBranchName),
                includedFailedReason = I18Variable(
                    code = includedFailedCode,
                    params = listOf(triggerOnBranchName)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = excludedFailedCode,
                    params = listOf(triggerOnBranchName)
                ).toJsonStr()
            )
            return listOf(userFilter, branchFilter)
        }
    }

    private fun GithubCreateEvent.getI18nCodeAndLinkUrl() = if (createType() == GithubCreateRefType.TAG) {
        WebhookI18nConstants.GITHUB_CREATE_TAG_EVENT_DESC to
                "${repository.getRepoUrl()}/releases/tag/$ref"
    } else {
        WebhookI18nConstants.GITHUB_CREATE_BRANCH_EVENT_DESC to
                "${repository.getRepoUrl()}/tree/$ref"
    }
}
