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

package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleEvent
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.pojo.enums.toGitState
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.util.GitCommonUtils

@Suppress("ALL")
class StreamScheduleAction(
    private val streamGitConfig: StreamGitConfig,
    private val gitCheckService: GitCheckService
) : BaseAction {

    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.SCHEDULE)

    override lateinit var data: ActionData
    fun event() = data.event as StreamScheduleEvent

    override lateinit var api: StreamGitApiService

    override fun init(): BaseAction? {
        return initCommonData()
    }

    private fun initCommonData(): StreamScheduleAction {
        val event = event()
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.gitProjectId,
            userId = event.userId,
            branch = event.branch.removePrefix("refs/heads/"),
            commit = EventCommonDataCommit(
                commitId = event.commitId,
                commitMsg = event.commitMsg,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(null),
                commitAuthorName = event.commitAuthor
            ),
            gitProjectName = null,
            scmType = null
        )
        return this
    }

    override fun getProjectCode(gitProjectId: String?) = if (gitProjectId != null) {
        GitCommonUtils.getCiProjectId(
            gitProjectId.toLong(),
            streamGitConfig.getScmType()
        )
    } else {
        event().projectCode
    }

    override fun getGitProjectIdOrName(gitProjectId: String?) = gitProjectId ?: data.eventCommon.gitProjectId

    override fun getGitCred(personToken: String?): StreamGitCred {
        return when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> TGitCred(
                userId = event().userId,
                accessToken = personToken,
                useAccessToken = personToken == null
            )
            ScmType.GITHUB -> GithubCred(
                userId = event().userId,
                accessToken = personToken,
                useAccessToken = personToken == null
            )
            else -> TODO("对接其他Git平台时需要补充")
        }
    }

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        // 手动触发保存下自定的定时触发事件，方便构建结束后逻辑
        return GitRequestEventHandle.createScheduleTriggerEvent(
            event = event(),
            eventStr = JsonUtil.toJson(data.event)
        )
    }

    override fun skipStream() = false

    override fun checkProjectConfig() {}

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>) = true

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {}

    override fun getYamlPathList(): List<YamlPathListEntry> {
        return emptyList()
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

    override fun getChangeSet(): Set<String>? {
        return null
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        return TriggerResult(
            trigger = TriggerBody(true),
            triggerOn = null,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? = null

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = true

    override fun needUpdateLastModifyUser(filePath: String) = false

    override fun sendCommitCheck(
        buildId: String,
        gitProjectName: String,
        state: StreamCommitCheckState,
        block: Boolean,
        context: String,
        targetUrl: String,
        description: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>
    ) {
        gitCheckService.pushCommitCheck(
            userId = data.getUserId(),
            projectCode = getProjectCode(),
            buildId = buildId,
            gitProjectId = data.eventCommon.gitProjectId,
            gitProjectName = gitProjectName,
            pipelineId = data.context.pipeline!!.pipelineId,
            commitId = data.eventCommon.commit.commitId,
            gitHttpUrl = data.setting.gitHttpUrl,
            scmType = streamGitConfig.getScmType(),
            token = api.getToken(getGitCred()),
            state = state.toGitState(streamGitConfig.getScmType()),
            block = block,
            context = context,
            targetUrl = targetUrl,
            description = description,
            mrId = null,
            reportData = reportData,
            addCommitCheck = api::addCommitCheck
        )
    }

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) {}

    override fun updatePipelineLastBranchAndDisplayName(pipelineId: String, branch: String?, displayName: String?) {}

    override fun getStartType() = StartType.TIME_TRIGGER

    override fun checkIfModify() = true
}
