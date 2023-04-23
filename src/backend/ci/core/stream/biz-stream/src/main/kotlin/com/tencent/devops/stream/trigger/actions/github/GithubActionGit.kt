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

package com.tencent.devops.stream.trigger.actions.github

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.pojo.enums.toGitState
import com.tencent.devops.stream.trigger.service.GitCheckService

/**
 * 对于stream 平台级功能的具体实现，不需要下放到具体的event
 * 对于只有一两个事件实现的，也可在平台级实现一个通用的，一两个再自己重写
 */
abstract class GithubActionGit(
    override val api: GithubApiService,
    private val gitCheckService: GitCheckService
) : GitBaseAction {
    override lateinit var data: ActionData

    override fun getProjectCode(gitProjectId: String?): String {
        return if (gitProjectId != null) {
            "github_$gitProjectId"
        } else {
            "github_${data.getGitProjectId()}"
        }
    }

    /**
     * 发现github有两套repo接口，对应 [repositories/:id] 和 [repos/:user/:projectName]
     * 目前先使用 repositories/:id 方案，有问题再切换
     */
    override fun getGitProjectIdOrName(gitProjectId: String?) =
        gitProjectId ?: data.context.pipeline?.gitProjectId ?: data.eventCommon.gitProjectId

    override fun getGitCred(personToken: String?): GithubCred {
        if (personToken != null) {
            return GithubCred(
                userId = null,
                accessToken = personToken,
                useAccessToken = false
            )
        }
        return GithubCred(data.setting.enableUser)
    }

    override fun getChangeSet(): Set<String>? = null

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? = null

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = true

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) = Unit

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
            scmType = ScmType.GITHUB,
            token = api.getToken(getGitCred()),
            state = state.toGitState(ScmType.GITHUB),
            block = block,
            context = context,
            targetUrl = targetUrl,
            description = description,
            mrId = if (data.event is GitMergeRequestEvent) {
                (data.event as GitMergeRequestEvent).object_attributes.iid
            } else {
                null
            },
            manualUnlock = if (data.event is GitMergeRequestEvent) {
                (data.event as GitMergeRequestEvent).manual_unlock
            } else {
                false
            },
            reportData = reportData,
            addCommitCheck = api::addCommitCheck
        )
    }

    override fun updatePipelineLastBranchAndDisplayName(pipelineId: String, branch: String?, displayName: String?) =
        Unit

    override fun parseStreamTriggerContext(cred: StreamGitCred?) {
        // 格式化repoCreatedTime
        this.data.context.repoCreatedTime = DateTimeUtil.formatDate(
            DateTimeUtil.zoneDateToDate(this.data.context.repoCreatedTime)!!
        )
    }
}
