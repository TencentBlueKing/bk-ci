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

package com.tencent.devops.stream.trigger.actions.tgit.data

import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.StreamTriggerContext
import com.tencent.devops.stream.trigger.actions.tgit.TGitActionCommon
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCommitInfo

data class TGitReviewActionData(
    override val event: GitReviewEvent,
    override val context: StreamTriggerContext = StreamTriggerContext()
) : ActionData {
    override lateinit var eventCommon: EventCommonData
    override lateinit var setting: StreamTriggerSetting
}

class TGitReviewEventCommonData(
    val event: GitReviewEvent,
    private val defaultBranch: String,
    val latestCommit: TGitCommitInfo?
) : EventCommonData {
    override val gitProjectId: String
        get() = event.projectId.toString()

    override val branch: String
        get() = defaultBranch

    override val commit: EventCommonDataCommit
        get() = EventCommonDataCommit(
            commitId = latestCommit?.commitId ?: "0",
            commitMsg = latestCommit?.commitMsg,
            commitTimeStamp = TGitActionCommon.getCommitTimeStamp(latestCommit?.commitDate),
            commitAuthorName = latestCommit?.commitAuthor
        )

    override val userId: String
        get() = if (event.reviewer == null) {
            event.author.username
        } else {
            event.reviewer!!.reviewer.username
        }

    override val gitProjectName: String
        get() = GitUtils.getProjectName(event.repository.homepage)
}
