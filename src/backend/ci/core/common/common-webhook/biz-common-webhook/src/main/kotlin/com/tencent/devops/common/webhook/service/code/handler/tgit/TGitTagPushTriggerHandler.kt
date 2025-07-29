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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TAG_DESC
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TAG_FROM
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TAG_MESSAGE
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_OPERATION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_USERNAME
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.DELETE_EVENT
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteTag
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.GitUrlFilter
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
class TGitTagPushTriggerHandler constructor(
    private val eventCacheService: EventCacheService
) : CodeWebhookTriggerHandler<GitTagPushEvent> {

    override fun eventClass(): Class<GitTagPushEvent> {
        return GitTagPushEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.TAG_PUSH
    }

    override fun getUrl(event: GitTagPushEvent): String {
        return event.repository.git_http_url
    }

    override fun getUsername(event: GitTagPushEvent): String {
        return event.user_name
    }

    override fun getRevision(event: GitTagPushEvent): String {
        return if (event.commits.isNullOrEmpty()) {
            ""
        } else {
            event.commits!![0].id
        }
    }

    override fun getRepoName(event: GitTagPushEvent): String {
        return GitUtils.getProjectName(event.repository.git_ssh_url)
    }

    override fun getBranchName(event: GitTagPushEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getMessage(event: GitTagPushEvent): String {
        return event.ref.replace("refs/tags/", "")
    }

    override fun getEventDesc(event: GitTagPushEvent): String {
        val createFrom = when {
            event.isDeleteTag() -> {
                // 删除标签时展示删除前的tag提交点
                GitUtils.getShortSha(event.before)
            }
            event.create_from.isNullOrBlank() || event.checkout_sha == event.create_from -> {
                GitUtils.getShortSha(event.checkout_sha)
            }

            else -> {
                event.create_from
            }
        }
        return I18Variable(
            code = if (event.isDeleteTag()) {
                WebhookI18nConstants.TGIT_TAG_DELETE_EVENT_DESC
            } else {
                WebhookI18nConstants.TGIT_TAG_PUSH_EVENT_DESC
            },
            params = listOf(
                "$createFrom",
                "${event.repository.homepage}/-/tags/${getBranchName(event)}",
                getBranchName(event),
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: GitTagPushEvent): String {
        return event.project_id.toString()
    }

    override fun retrieveParams(
        event: GitTagPushEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        val tagName = getBranchName(event)
        startParams[BK_REPO_GIT_WEBHOOK_TAG_NAME] = tagName
        startParams[BK_REPO_GIT_WEBHOOK_TAG_OPERATION] = event.operation_kind ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = event.total_commits_count
        startParams[BK_REPO_GIT_WEBHOOK_TAG_USERNAME] = event.user_name
        startParams[BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM] = event.create_from ?: ""
        startParams.putAll(WebhookUtils.genCommitsParam(commits = event.commits ?: emptyList()))

        // 兼容stream变量
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.git_http_url
        startParams[PIPELINE_GIT_REF] = event.ref
        startParams[CI_BRANCH] = tagName
        startParams[PIPELINE_GIT_EVENT] = if (event.isDeleteTag()) {
            DELETE_EVENT
        } else {
            GitTagPushEvent.classType
        }
        startParams[PIPELINE_GIT_COMMIT_AUTHOR] =
            event.commits?.firstOrNull()?.author?.name ?: ""
        startParams[PIPELINE_GIT_BEFORE_SHA] = event.before
        startParams[PIPELINE_GIT_BEFORE_SHA_SHORT] = GitUtils.getShortSha(event.before)
        startParams[PIPELINE_GIT_COMMIT_MESSAGE] = event.commits?.firstOrNull()?.message ?: ""
        if (!event.create_from.isNullOrBlank()) {
            startParams[PIPELINE_GIT_TAG_FROM] = event.create_from!!
        }
        startParams[PIPELINE_GIT_EVENT_URL] = "${event.repository.homepage}/-/tags/$tagName"
        startParams[PIPELINE_GIT_TAG_MESSAGE] = event.message ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = tagName
        startParams[PIPELINE_GIT_ACTION] = event.operation_kind ?: ""
        // Tag 详细信息
        if (!projectId.isNullOrBlank() && repository != null) {
            val tagDesc = eventCacheService.getTagInfo(
                projectId = projectId,
                repo = repository,
                tagName = tagName
            )?.description
            if (!tagDesc.isNullOrEmpty()) {
                startParams[PIPELINE_GIT_TAG_DESC] = if (tagDesc.length > TAG_DESC_MAX_LENGTH) {
                    tagDesc.substring(0, TAG_DESC_MAX_LENGTH)
                } else {
                    tagDesc
                }
            }
        }
        return startParams
    }

    override fun getWebhookFilters(
        event: GitTagPushEvent,
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
            val eventTag = getBranchName(event)
            val branchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = eventTag,
                includedBranches = WebhookUtils.convert(tagName),
                excludedBranches = WebhookUtils.convert(excludeTagName),
                includedFailedReason = I18Variable(
                    code = WebhookI18nConstants.TAG_NAME_NOT_MATCH,
                    params = listOf(eventTag)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = WebhookI18nConstants.TAG_NAME_IGNORED,
                    params = listOf(eventTag)
                ).toJsonStr()
            )
            val userId = getUsername(event)
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = getUsername(event),
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
            val fromBranch = event.create_from ?: ""
            val fromBranchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = fromBranch,
                includedBranches = WebhookUtils.convert(fromBranches),
                excludedBranches = emptyList(),
                includedFailedReason = I18Variable(
                    code = WebhookI18nConstants.TAG_SOURCE_BRANCH_NOT_MATCH,
                    params = listOf(fromBranch)
                ).toJsonStr(),
                excludedFailedReason = ""
            )
            return listOf(urlFilter, eventTypeFilter, branchFilter, userFilter, fromBranchFilter)
        }
    }

    companion object {
        const val TAG_DESC_MAX_LENGTH = 2000
    }
}
