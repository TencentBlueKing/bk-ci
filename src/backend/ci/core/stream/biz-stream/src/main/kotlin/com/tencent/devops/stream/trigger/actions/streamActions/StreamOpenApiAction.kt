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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBuilder
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

@Suppress("ALL")
class StreamOpenApiAction(
    private val action: BaseAction,
    private val checkPipelineTrigger: Boolean
) : BaseAction {
    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.OPENAPI)
    override var data: ActionData = action.data
    override val api: StreamGitApiService = action.api

    override fun init(): BaseAction? {
        action.init()
        return this
    }

    override fun getProjectCode(gitProjectId: String?) = action.getProjectCode()
    override fun getGitProjectIdOrName(gitProjectId: String?) = action.getGitProjectIdOrName(gitProjectId)

    override fun getGitCred(personToken: String?) = action.getGitCred()

    override fun buildRequestEvent(eventStr: String) = action.buildRequestEvent(eventStr)

    override fun skipStream(): Boolean = action.skipStream()

    override fun checkProjectConfig() = action.checkProjectConfig()

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>) =
        action.checkMrConflict(path2PipelineExists)

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        action.checkAndDeletePipeline(path2PipelineExists)
    }

    override fun getYamlPathList(): List<YamlPathListEntry> = action.getYamlPathList()

    override fun getYamlContent(fileName: String) = action.getYamlContent(fileName)
    override fun getChangeSet(): Set<String>? {
        return action.getChangeSet()
    }

    override fun isMatch(triggerOn: TriggerOn) = action.isMatch(triggerOn)

    override fun checkIfModify() = action.checkIfModify()

    fun getStartParams(scmType: ScmType): Map<String, String> {
        return when (scmType) {
            ScmType.CODE_GIT -> {
                GitActionCommon.getStartParams(
                    action = action,
                    triggerOn = TriggerBuilder.buildManualTriggerOn(action.metaData.streamObjectKind)
                )
            }
            else -> TODO("对接其他Git平台时需要补充")
        }
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?) = action.getUserVariables(yamlVariables)

    override fun needSaveOrUpdateBranch() = action.needSaveOrUpdateBranch()

    override fun needSendCommitCheck() = false

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
    }

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) {
        action.registerCheckRepoTriggerCredentials(repoHook)
    }

    override fun needAddWebhookParams() = action is GitBaseAction

    override fun updatePipelineLastBranchAndDisplayName(pipelineId: String, branch: String?, displayName: String?) =
        Unit

    override fun getStartType() = if (checkPipelineTrigger) StartType.PIPELINE else StartType.SERVICE
}
