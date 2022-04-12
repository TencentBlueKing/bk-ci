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

import com.devops.process.yaml.v2.enums.StreamObjectKind
import com.devops.process.yaml.v2.models.Variable
import com.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualActionData
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualCommonData
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

class StreamManualAction(
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache
) : BaseAction {

    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.MANUAL)

    override lateinit var data: ActionData
    fun data() = data as StreamManualActionData

    override lateinit var api: StreamGitApiService

    override fun getProjectCode(gitProjectId: String?) = data().event.projectCode

    override fun getGitCred(personToken: String?): StreamGitCred {
        return when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> TGitCred(
                userId = data().event.userId,
                accessToken = personToken,
                useAccessToken = personToken == null
            )
            else -> TODO("对接其他Git平台时需要补充")
        }
    }

    fun initCommonData(): StreamManualAction {
        val defaultBranch = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = data().event.gitProjectId,
            action = this,
            getProjectInfo = api::getGitProjectInfo
        ).defaultBranch!!
        val latestCommit = api.getGitCommitInfo(
            cred = this.getGitCred(),
            gitProjectId = data().event.gitProjectId,
            sha = defaultBranch,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = StreamManualCommonData(data().event, latestCommit)
        return this
    }

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val data = data()
        // 手动触发保存下自定的手动触发事件，方便构建结束后逻辑
        return GitRequestEventHandle.createManualTriggerEvent(
            event = data.event,
            latestCommit = (data.eventCommon as StreamManualCommonData).latestCommit,
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
        return TriggerResult(
            trigger = true,
            startParams = emptyMap(),
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? {
        return null
    }

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = false

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
    }
}
