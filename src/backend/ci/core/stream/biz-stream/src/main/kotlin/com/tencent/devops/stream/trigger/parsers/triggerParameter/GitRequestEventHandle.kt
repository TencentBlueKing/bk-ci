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

package com.tencent.devops.stream.trigger.parsers.triggerParameter

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.webhook.enums.code.tgit.TGitIssueAction
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitReviewEventKind
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteBranch
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteTag
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleEvent
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.Date
import javax.ws.rs.core.Response

object GitRequestEventHandle {

    fun createPushEvent(gitPushEvent: GitPushEvent, e: String): GitRequestEvent {
        val latestCommit = if (gitPushEvent.isDeleteBranch()) {
            // 删除事件，暂时无法获取到latest commit 相关信息
            null
        } else {
            getLatestCommit(
                gitPushEvent.after,
                gitPushEvent.commits
            )
        }
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.PUSH.value,
            operationKind = gitPushEvent.operation_kind,
            extensionAction = gitPushEvent.action_kind,
            gitProjectId = gitPushEvent.project_id,
            sourceGitProjectId = null,
            branch = gitPushEvent.ref.removePrefix("refs/heads/"),
            targetBranch = null,
            commitId = gitPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            commitAuthorName = latestCommit?.author?.name,
            userId = gitPushEvent.user_name,
            totalCommitCount = gitPushEvent.total_commits_count.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null,
            gitEvent = gitPushEvent,
            gitProjectName = GitUtils.getProjectName(gitPushEvent.repository.homepage)
        )
    }

    fun createMergeEvent(gitMrEvent: GitMergeRequestEvent, e: String): GitRequestEvent {
        val latestCommit = gitMrEvent.object_attributes.last_commit
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.MERGE_REQUEST.value,
            operationKind = null,
            extensionAction = gitMrEvent.object_attributes.extension_action,
            gitProjectId = gitMrEvent.object_attributes.target_project_id,
            sourceGitProjectId = gitMrEvent.object_attributes.source_project_id,
            // Merged动作使用目标分支，因为源分支可能已被删除
            branch = if (gitMrEvent.object_attributes.action == TGitMergeActionKind.MERGE.value) {
                gitMrEvent.object_attributes.target_branch
            } else {
                gitMrEvent.object_attributes.source_branch
            },
            targetBranch = gitMrEvent.object_attributes.target_branch,
            commitId = latestCommit.id,
            commitMsg = latestCommit.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit.timestamp),
            commitAuthorName = latestCommit.author.name,
            userId = gitMrEvent.user.username,
            totalCommitCount = 0,
            mergeRequestId = gitMrEvent.object_attributes.iid,
            event = e,
            description = "",
            mrTitle = gitMrEvent.object_attributes.title,
            gitEvent = gitMrEvent,
            gitProjectName = GitUtils.getProjectName(gitMrEvent.object_attributes.target.http_url)
        )
    }

    fun createTagPushEvent(gitTagPushEvent: GitTagPushEvent, e: String): GitRequestEvent {
        val latestCommit = if (gitTagPushEvent.isDeleteTag()) {
            // 删除事件，暂时无法获取到latest commit 相关信息
            null
        } else {
            getLatestCommit(
                null,
                gitTagPushEvent.commits
            )
        }
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.TAG_PUSH.value,
            operationKind = gitTagPushEvent.operation_kind,
            extensionAction = null,
            gitProjectId = gitTagPushEvent.project_id,
            sourceGitProjectId = null,
            branch = gitTagPushEvent.ref.removePrefix("refs/tags/"),
            targetBranch = null,
            commitId = gitTagPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            commitAuthorName = latestCommit?.author?.name,
            userId = gitTagPushEvent.user_name,
            totalCommitCount = gitTagPushEvent.total_commits_count.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null,
            gitEvent = gitTagPushEvent,
            gitProjectName = GitUtils.getProjectName(gitTagPushEvent.repository.homepage)
        )
    }

    fun createIssueEvent(
        gitIssueEvent: GitIssueEvent,
        e: String,
        defaultBranch: String,
        latestCommit: EventCommonDataCommit?
    ): GitRequestEvent {
        val gitProjectId = gitIssueEvent.objectAttributes.projectId
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.ISSUE.value,
            operationKind = "",
            extensionAction = TGitIssueAction.getDesc(gitIssueEvent.objectAttributes.action ?: ""),
            gitProjectId = gitProjectId,
            sourceGitProjectId = null,
            branch = defaultBranch,
            targetBranch = null,
            commitId = latestCommit?.commitId ?: "0",
            commitMsg = gitIssueEvent.objectAttributes.title,
            commitTimeStamp = latestCommit?.commitTimeStamp,
            commitAuthorName = latestCommit?.commitAuthorName,
            userId = gitIssueEvent.user.username,
            totalCommitCount = 1,
            mergeRequestId = gitIssueEvent.objectAttributes.iid.toLong(),
            event = e,
            description = "",
            mrTitle = gitIssueEvent.objectAttributes.title,
            gitEvent = gitIssueEvent,
            gitProjectName = GitUtils.getProjectName(gitIssueEvent.repository.homepage)
        )
    }

    fun createNoteEvent(
        gitNoteEvent: GitNoteEvent,
        e: String,
        defaultBranch: String,
        latestCommit: EventCommonDataCommit?
    ): GitRequestEvent {
        val gitProjectId = gitNoteEvent.objectAttributes.projectId
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.NOTE.value,
            operationKind = "",
            extensionAction = "submitted",
            gitProjectId = gitProjectId,
            sourceGitProjectId = null,
            branch = defaultBranch,
            targetBranch = null,
            commitId = latestCommit?.commitId ?: "0",
            commitMsg = gitNoteEvent.objectAttributes.note,
            commitTimeStamp = latestCommit?.commitTimeStamp,
            commitAuthorName = latestCommit?.commitAuthorName,
            userId = gitNoteEvent.user.username,
            totalCommitCount = 1,
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null,
            gitEvent = gitNoteEvent,
            gitProjectName = GitUtils.getProjectName(gitNoteEvent.repository.homepage)
        )
    }

    fun createReviewEvent(
        gitReviewEvent: GitReviewEvent,
        e: String,
        defaultBranch: String,
        latestCommit: EventCommonDataCommit?
    ): GitRequestEvent {
        val gitProjectId = gitReviewEvent.projectId
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.REVIEW.value,
            operationKind = "",
            extensionAction = when (gitReviewEvent.event) {
                TGitReviewEventKind.CREATE.value -> "created"
                TGitReviewEventKind.INVITE.value -> "updated"
                else -> gitReviewEvent.state
            },
            gitProjectId = gitProjectId,
            sourceGitProjectId = null,
            branch = defaultBranch,
            targetBranch = null,
            commitId = latestCommit?.commitId ?: "0",
            commitMsg = latestCommit?.commitMsg,
            commitTimeStamp = latestCommit?.commitTimeStamp,
            commitAuthorName = latestCommit?.commitAuthorName,
            userId = if (gitReviewEvent.reviewer == null) {
                gitReviewEvent.author.username
            } else {
                gitReviewEvent.reviewer!!.reviewer.username
            },
            totalCommitCount = 1,
            mergeRequestId = gitReviewEvent.iid.toLong(),
            event = e,
            description = "",
            mrTitle = "",
            gitEvent = gitReviewEvent,
            gitProjectName = GitUtils.getProjectName(gitReviewEvent.repository.homepage)
        )
    }

    fun createManualTriggerEvent(
        event: StreamManualEvent,
        latestCommit: EventCommonDataCommit?,
        eventStr: String
    ): GitRequestEvent {
        if (event.branch.isBlank()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "branche cannot be empty"
            )
        }

        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.MANUAL.value,
            operationKind = "",
            extensionAction = null,
            gitProjectId = event.gitProjectId.toLong(),
            sourceGitProjectId = null,
            branch = getBranchName(event.branch),
            targetBranch = null,
            commitId = event.commitId ?: (latestCommit?.commitId ?: ""),
            commitMsg = event.customCommitMsg,
            commitTimeStamp = latestCommit?.commitTimeStamp ?: getCommitTimeStamp(null),
            commitAuthorName = event.userId,
            userId = event.userId,
            totalCommitCount = 0,
            mergeRequestId = null,
            event = eventStr,
            description = event.description,
            mrTitle = "",
            gitEvent = null,
            gitProjectName = null
        )
    }

    fun createScheduleTriggerEvent(
        event: StreamScheduleEvent,
        eventStr: String
    ): GitRequestEvent {
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.SCHEDULE.value,
            operationKind = null,
            extensionAction = null,
            gitProjectId = event.gitProjectId.toLong(),
            sourceGitProjectId = null,
            branch = event.branch,
            targetBranch = null,
            commitId = event.commitId,
            commitMsg = event.commitMsg,
            commitTimeStamp = getCommitTimeStamp(null),
            commitAuthorName = event.commitAuthor,
            userId = event.userId,
            totalCommitCount = 0,
            mergeRequestId = null,
            event = eventStr,
            description = null,
            mrTitle = null,
            gitEvent = null,
            gitProjectName = null
        )
    }

    private fun getCommitTimeStamp(commitTimeStamp: String?): String {
        return if (commitTimeStamp.isNullOrBlank()) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            formatter.format(Date())
        } else {
            val time = DateTime.parse(commitTimeStamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            sdf.format(time.toDate())
        }
    }

    private fun getBranchName(ref: String): String {
        return when {
            ref.startsWith("refs/heads/") ->
                ref.removePrefix("refs/heads/")
            ref.startsWith("refs/tags/") ->
                ref.removePrefix("refs/tags/")
            else -> ref
        }
    }

    private fun getLatestCommit(
        commitId: String?,
        commits: List<GitCommit>?
    ): GitCommit? {
        if (commitId == null) {
            return if (commits.isNullOrEmpty()) {
                null
            } else {
                commits.last()
            }
        }
        commits?.forEach {
            if (it.id == commitId) {
                return it
            }
        }
        return null
    }
}
