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

package com.tencent.devops.stream.trigger.actions.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteTag
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.GitCheckService
import org.slf4j.LoggerFactory

class TGitTagPushActionGit(
    private val apiService: TGitApiService,
    gitCheckService: GitCheckService
) : TGitActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        val logger = LoggerFactory.getLogger(TGitTagPushActionGit::class.java)!!
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.TAG_PUSH)

    override fun event() = data.event as GitTagPushEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction {
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val lastCommit = getLatestCommit(event)

        this.data.eventCommon = EventCommonData(
            gitProjectId = event.project_id.toString(),
            scmType = ScmType.CODE_GIT,
            branch = event.ref.removePrefix("refs/tags/"),
            commit = EventCommonDataCommit(
                commitId = event.after,
                commitMsg = lastCommit?.message,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(lastCommit?.timestamp),
                commitAuthorName = lastCommit?.author?.name
            ),
            userId = event.user_name,
            gitProjectName = GitUtils.getProjectName(event.repository.homepage)
        )
        return this
    }

    private fun getLatestCommit(
        event: GitTagPushEvent
    ): GitCommit? {
        var commit = if (event.commits.isNullOrEmpty()) {
            null
        } else {
            event.commits!!.first()
        }

        // 2022/7/1   外面的message不为空，为附录tag，取外面的
        if (event.message != null) {
            commit = commit?.copy(message = event.message!!)
        }

        return commit
    }

    override fun isStreamDeleteAction() = event().isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        if (!event().tagPushEventFilter()) {
            return null
        }
        return GitRequestEventHandle.createTagPushEvent(event(), eventStr)
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() {
        if (!data.setting.buildPushedBranches) {
            throw StreamTriggerException(this, TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        return true
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) = Unit

    override fun getYamlPathList(): List<YamlPathListEntry> {
        return GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = this.getGitProjectIdOrName(),
            ref = this.data.eventCommon.branch
        ).map { (name, blobId) ->
            YamlPathListEntry(name, CheckType.NO_NEED_CHECK, this.data.eventCommon.branch, blobId)
        }
    }

    override fun getYamlContent(fileName: String): YamlContent {
        return YamlContent(
            ref = data.eventCommon.branch,
            content = api.getFileContent(
                cred = this.getGitCred(),
                gitProjectId = getGitProjectIdOrName(),
                fileName = fileName,
                ref = data.eventCommon.branch,
                retry = ApiRequestRetryInfo(true)
            )
        )
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val event = event()
        val isMatch = TriggerMatcher.isTagPushMatch(
            triggerOn,
            GitActionCommon.getTriggerBranch(event.ref),
            data.eventCommon.userId,
            event.create_from
        )
        return TriggerResult(
            trigger = isMatch,
            triggerOn = triggerOn,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return GitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSendCommitCheck() = !event().isDeleteTag()
}

private fun GitTagPushEvent.tagPushEventFilter(): Boolean {
    if (isDeleteTag()) {
        return true
    }
    if (total_commits_count <= 0) {
        TGitTagPushActionGit.logger.info(
            "TGitTagPushActionGit|tagPushEventFilter" +
                "|Git tag web hook no commit($total_commits_count)"
        )
        return false
    }
    return true
}
