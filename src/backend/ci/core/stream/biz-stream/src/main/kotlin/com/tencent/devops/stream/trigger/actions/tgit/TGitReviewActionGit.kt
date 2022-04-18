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

import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitReviewActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitReviewEventCommonData
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.GitCheckService
import org.slf4j.LoggerFactory

class TGitReviewActionGit(
    private val apiService: TGitApiService,
    private val streamTriggerCache: StreamTriggerCache,
    private val gitCheckService: GitCheckService
) : TGitActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitReviewActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.REVIEW)

    override lateinit var data: ActionData
    fun data() = data as TGitReviewActionData

    override val api: TGitApiService
        get() = apiService

    override fun init(requestEventId: Long) {
        this.data.context.requestEventId = requestEventId
        initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = data().event
        val gitProjectId = event.projectId
        val defaultBranch = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = gitProjectId.toString(),
            action = this,
            getProjectInfo = apiService::getGitProjectInfo
        ).defaultBranch!!
        val latestCommit = apiService.getGitCommitInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId.toString(),
            sha = defaultBranch,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = TGitReviewEventCommonData(event, defaultBranch, latestCommit)
        return this
    }

    override fun isStreamDeleteAction() = data().event.isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val data = data()
        return GitRequestEventHandle.createReviewEvent(
            gitReviewEvent = data.event,
            e = eventStr,
            defaultBranch = data.eventCommon.branch,
            latestCommit = (data.eventCommon as TGitReviewEventCommonData).latestCommit
        )
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() {}

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
        val (isTrigger, startParams) = TGitActionCommon.matchAndStartParams(this, triggerOn)
        return TriggerResult(
            trigger = isTrigger,
            startParams = startParams,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return TGitActionCommon.matchAndStartParams(this, triggerOn).second
    }
}
