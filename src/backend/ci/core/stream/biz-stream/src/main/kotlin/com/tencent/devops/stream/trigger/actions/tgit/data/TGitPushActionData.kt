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

import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.StreamTriggerContext
import com.tencent.devops.stream.trigger.actions.tgit.TGitActionCommon

data class TGitPushActionData(
    override val event: GitPushEvent,
    override val context: StreamTriggerContext = StreamTriggerContext()
) : ActionData {
    override lateinit var eventCommon: EventCommonData
    override lateinit var setting: StreamTriggerSetting
}

class TGitPushEventCommonData(val event: GitPushEvent) : EventCommonData {

    override val gitProjectId: String
        get() = event.project_id.toString()

    override val branch: String
        get() = event.ref.removePrefix("refs/heads/")

    private val lastCommit = getLatestCommit(event)
    override val commit: EventCommonDataCommit
        get() = EventCommonDataCommit(
            commitId = event.after,
            commitMsg = lastCommit?.message,
            commitTimeStamp = TGitActionCommon.getCommitTimeStamp(lastCommit?.timestamp),
            commitAuthorName = lastCommit?.author?.name
        )

    override val userId: String
        get() = event.user_name

    override val gitProjectName: String
        get() = GitUtils.getProjectName(event.repository.homepage)

    private fun getLatestCommit(
        event: GitPushEvent
    ): GitCommit? {
        if (event.isDeleteEvent()) {
            return null
        }
        val commitId = event.after
        val commits = event.commits
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
