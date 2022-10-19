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
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_AUTHOR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_COMMENT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_CREATED_AT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_NOTEABLE_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_UPDATED_AT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_COMMIT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.CI_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_COMMENT
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.RegexContainFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
class TGitNoteTriggerHandler(
    private val eventCacheService: EventCacheService
) : GitHookTriggerHandler<GitNoteEvent> {

    override fun eventClass(): Class<GitNoteEvent> {
        return GitNoteEvent::class.java
    }

    override fun getUrl(event: GitNoteEvent): String {
        return event.repository.url
    }

    override fun getUsername(event: GitNoteEvent): String {
        return event.user.username
    }

    override fun getRevision(event: GitNoteEvent): String {
        return ""
    }

    override fun getRepoName(event: GitNoteEvent): String {
        return GitUtils.getProjectName(event.repository.url)
    }

    override fun getBranchName(event: GitNoteEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.NOTE
    }

    override fun getMessage(event: GitNoteEvent): String? {
        return event.objectAttributes.note
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    override fun retrieveParams(event: GitNoteEvent, projectId: String?, repository: Repository?): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(event.objectAttributes) {
            startParams[PIPELINE_WEBHOOK_NOTE_COMMENT] = note
            startParams[PIPELINE_WEBHOOK_NOTE_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_COMMENT] = note
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID] = projectId.toString()
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_NOTEABLE_TYPE] = noteableType
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_AUTHOR_ID] = authorId
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_CREATED_AT] = createdAt
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_UPDATED_AT] = updatedAt
            startParams[BK_REPO_GIT_WEBHOOK_NOTE_URL] = url
        }
        if (projectId != null && repository != null) {
            val (defaultBranch, commitInfo) =
                eventCacheService.getDefaultBranchLatestCommitInfo(projectId = projectId, repo = repository)
            startParams[PIPELINE_GIT_REF] = defaultBranch ?: ""
            startParams[CI_BRANCH] = defaultBranch ?: ""

            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = commitInfo?.author_name ?: ""
            startParams[PIPELINE_GIT_SHA] = commitInfo?.id ?: ""
            startParams[PIPELINE_GIT_SHA_SHORT] = commitInfo?.short_id ?: ""
            startParams[PIPELINE_GIT_BEFORE_SHA] = "----------"
            startParams[PIPELINE_GIT_BEFORE_SHA_SHORT] = "----------"
            startParams[PIPELINE_GIT_COMMIT_MESSAGE] = commitInfo?.message ?: ""
        }
        // 兼容stream变量
        startParams[PIPELINE_GIT_REPO_URL] = event.repository.homepage + ".git"
        startParams[PIPELINE_GIT_EVENT_URL] = event.objectAttributes.url
        startParams[PIPELINE_GIT_EVENT] = GitNoteEvent.classType
        event.commit?.apply {
            startParams[PIPELINE_GIT_COMMIT_AUTHOR] = author.name
            startParams[PIPELINE_GIT_SHA] = id
            startParams[PIPELINE_GIT_SHA_SHORT] = id.substring(0, 8)
            startParams[PIPELINE_GIT_COMMIT_MESSAGE] = message
        }
        event.mergeRequest?.apply {
            val mrRequestId = if (repository is CodeGitlabRepository) {
                iid
            } else {
                id
            }
            startParams[PIPELINE_GIT_MR_ACTION] = action ?: ""
            if (projectId == null || repository == null) {
                return@apply
            }
            // MR提交人
            val mrInfo = eventCacheService.getMergeRequestInfo(projectId, mrRequestId, repository)
            val reviewInfo =
                eventCacheService.getMergeRequestReviewersInfo(projectId, mrRequestId, repository)
            startParams.putAll(
                WebhookUtils.mrStartParam(
                    mrInfo = mrInfo,
                    reviewInfo = reviewInfo,
                    mrRequestId = mrRequestId,
                    homepage = event.repository.homepage
                )
            )
        }
        event.issue?.apply {
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_TITLE] = title
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_IID] = iid
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION] = description ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_STATE] = state
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_OWNER] = event.user.username
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_URL] = url ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID] = milestoneId ?: 0L
        }
        event.review?.apply {
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_STATE] = state
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_ID] = id
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_IID] = iid
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_BRANCH] = sourceBranch
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_PROJECT_ID] = sourceProjectId
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_SOURCE_COMMIT] = sourceCommit
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_COMMIT] = targetCommit
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_BRANCH] = targetBranch
            startParams[BK_REPO_GIT_WEBHOOK_REVIEW_TARGET_PROJECT_ID] = targetProjectId
        }
        return startParams
    }

    override fun getEventFilters(
        event: GitNoteEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        val typeActionFilter = ContainsFilter(
            pipelineId = pipelineId,
            filterName = "noteTypeAction",
            triggerOn = event.objectAttributes.noteableType,
            included = WebhookUtils.convert(webHookParams.includeNoteTypes)
        )
        val commentActionFilter = RegexContainFilter(
            pipelineId = pipelineId,
            filterName = "noteCommentAction",
            triggerOn = event.objectAttributes.note,
            included = WebhookUtils.convert(webHookParams.includeNoteComment)
        )
        return listOf(typeActionFilter, commentActionFilter)
    }
}
