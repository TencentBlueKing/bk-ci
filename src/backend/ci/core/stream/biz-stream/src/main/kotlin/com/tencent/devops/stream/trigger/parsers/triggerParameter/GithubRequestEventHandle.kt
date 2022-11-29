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

import com.tencent.devops.common.webhook.enums.code.github.GithubPrEventAction
import com.tencent.devops.common.webhook.enums.code.github.GithubPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.pojo.GitRequestEvent
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.Date

object GithubRequestEventHandle {

    fun createPushEvent(gitPushEvent: GithubPushEvent, e: String): GitRequestEvent {
        val latestCommit = if (gitPushEvent.deleted) {
            // 删除事件，暂时无法获取到latest commit 相关信息
            null
        } else {
            gitPushEvent.headCommit
        }
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.PUSH.value,
            operationKind = GithubPushOperationKind.getOperationKind(gitPushEvent).value,
            extensionAction = "",
            gitProjectId = gitPushEvent.repository.id.toLong(),
            sourceGitProjectId = null,
            branch = gitPushEvent.ref.removePrefix("refs/heads/"),
            targetBranch = null,
            commitId = gitPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            commitAuthorName = latestCommit?.author?.name,
            userId = gitPushEvent.sender.login,
            totalCommitCount = gitPushEvent.commits.size.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null,
            gitEvent = gitPushEvent,
            gitProjectName = gitPushEvent.repository.fullName
        )
    }

    fun createMergeEvent(gitMrEvent: GithubPullRequestEvent, e: String): GitRequestEvent {
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.PULL_REQUEST.value,
            operationKind = null,
            extensionAction = GithubPrEventAction.get(gitMrEvent).value,
            gitProjectId = gitMrEvent.pullRequest.base.repo.id.toLong(),
            sourceGitProjectId = gitMrEvent.pullRequest.head.repo.id.toLong(),
            // Merged动作使用目标分支，因为源分支可能已被删除
            branch = if (gitMrEvent.pullRequest.merged == true) {
                gitMrEvent.pullRequest.base.ref
            } else {
                gitMrEvent.pullRequest.head.ref
            },
            targetBranch = gitMrEvent.pullRequest.base.ref,
            commitId = gitMrEvent.pullRequest.head.sha,
            commitMsg = null,
            commitTimeStamp = null,
            commitAuthorName = gitMrEvent.pullRequest.head.user.login,
            userId = gitMrEvent.sender.login,
            totalCommitCount = 0,
            mergeRequestId = gitMrEvent.pullRequest.number.toLong(),
            event = e,
            description = "",
            mrTitle = gitMrEvent.pullRequest.title,
            gitEvent = gitMrEvent,
            gitProjectName = gitMrEvent.pullRequest.base.repo.fullName
        )
    }

    fun createTagPushEvent(gitTagPushEvent: GithubPushEvent, e: String): GitRequestEvent {
        val latestCommit = if (gitTagPushEvent.deleted) {
            // 删除事件，暂时无法获取到latest commit 相关信息
            null
        } else {
            gitTagPushEvent.headCommit
        }
        return GitRequestEvent(
            id = null,
            objectKind = StreamObjectKind.TAG_PUSH.value,
            operationKind = GithubPushOperationKind.getOperationKind(gitTagPushEvent).value,
            extensionAction = null,
            gitProjectId = gitTagPushEvent.repository.id.toLong(),
            sourceGitProjectId = null,
            branch = gitTagPushEvent.ref.removePrefix("refs/tags/"),
            targetBranch = null,
            commitId = gitTagPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            commitAuthorName = latestCommit?.author?.name,
            userId = gitTagPushEvent.sender.login,
            totalCommitCount = gitTagPushEvent.commits.size.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null,
            gitEvent = gitTagPushEvent,
            gitProjectName = gitTagPushEvent.repository.fullName
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
}
