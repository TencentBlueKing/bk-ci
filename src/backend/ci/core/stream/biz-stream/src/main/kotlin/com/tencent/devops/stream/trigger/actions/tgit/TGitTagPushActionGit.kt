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

import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteTag
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitTagPushActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitTagPushEventCommonData
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.GitCheckService
import org.slf4j.LoggerFactory

class TGitTagPushActionGit(
    private val apiService: TGitApiService,
    private val gitCheckService: GitCheckService
) : TGitActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        val logger = LoggerFactory.getLogger(TGitTagPushActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.TAG_PUSH)

    override lateinit var data: ActionData
    fun data() = data as TGitTagPushActionData

    override val api: TGitApiService
        get() = apiService

    override fun init() {
        initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        this.data.eventCommon = TGitTagPushEventCommonData(data().event)
        return this
    }

    override fun isStreamDeleteAction() = data().event.isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val rData = data()
        if (!rData.event.tagPushEventFilter()) {
            return null
        }
        return GitRequestEventHandle.createTagPushEvent(rData.event, eventStr)
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() {
        if (!data().setting.buildPushedBranches) {
            throw StreamTriggerException(this, TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        return true
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {}

    override fun getYamlPathList(): List<YamlPathListEntry> {
        return TGitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = this.data().getGitProjectId(),
            ref = this.data.eventCommon.branch
        ).map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }
    }

    override fun getYamlContent(fileName: String): String {
        return api.getFileContent(
            cred = this.getGitCred(),
            gitProjectId = data.getGitProjectId(),
            fileName = fileName,
            ref = data.eventCommon.branch,
            retry = ApiRequestRetryInfo(true)
        )
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val event = data().event
        val isMatch = TriggerMatcher.isTagPushMatch(
            triggerOn,
            TGitActionCommon.getTriggerBranch(event.ref),
            data.eventCommon.userId,
            event.create_from
        )
        val params = TGitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
        return TriggerResult(
            trigger = isMatch,
            startParams = params,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return TGitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSendCommitCheck() = !data().event.isDeleteTag()
}

private fun GitTagPushEvent.tagPushEventFilter(): Boolean {
    if (isDeleteTag()) {
        return true
    }
    if (total_commits_count <= 0) {
        TGitTagPushActionGit.logger.info("Git tag web hook no commit($total_commits_count)")
        return false
    }
    return true
}
