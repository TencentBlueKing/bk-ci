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
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.GitCheckService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class TGitIssueActionGit(
    private val dslContext: DSLContext,
    private val apiService: TGitApiService,
    gitCheckService: GitCheckService,
    private val basicSettingDao: StreamBasicSettingDao
) : TGitActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitIssueActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(
        streamObjectKind = StreamObjectKind.ISSUE
    )

    override fun event() = data.event as GitIssueEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction? {
        if (data.isSettingInitialized) {
            return initCommonData()
        }
        val setting = basicSettingDao.getSetting(dslContext, event().objectAttributes.projectId)
        if (null == setting || !setting.enableCi) {
            logger.info(
                "TGitIssueActionGit|init|not enabled, but trigger|git project id|${event().objectAttributes.projectId}"
            )
            return null
        }
        data.setting = StreamTriggerSetting(setting)
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val gitProjectId = event.objectAttributes.projectId

        val defaultBranch = apiService.getGitProjectInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId.toString(),
            retry = ApiRequestRetryInfo(true)
        )!!.defaultBranch!!
        val latestCommit = apiService.getGitCommitInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId.toString(),
            sha = defaultBranch,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.objectAttributes.projectId.toString(),
            scmType = ScmType.CODE_GIT,
            branch = defaultBranch,
            commit = EventCommonDataCommit(
                commitId = latestCommit?.commitId ?: "0",
                commitMsg = event.objectAttributes.title,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(latestCommit?.commitDate),
                commitAuthorName = latestCommit?.commitAuthor
            ),
            userId = event.user.username,
            gitProjectName = GitUtils.getProjectName(event.repository.homepage)
        )
        this.data.context.gitDefaultBranchLatestCommitInfo = defaultBranch to latestCommit?.toGitCommit()
        return this
    }

    override fun isStreamDeleteAction() = event().isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent {
        return GitRequestEventHandle.createIssueEvent(
            gitIssueEvent = event(),
            e = eventStr,
            defaultBranch = data.eventCommon.branch,
            latestCommit = data.eventCommon.commit
        )
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() = Unit

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
        val (isTrigger, _) = GitActionCommon.matchAndStartParams(
            action = this,
            triggerOn = triggerOn,
            onlyMatch = true
        )
        return TriggerResult(
            trigger = TriggerBody(isTrigger),
            triggerOn = triggerOn,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return GitActionCommon.matchAndStartParams(this, triggerOn).second
    }
}
