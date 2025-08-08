/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.process.yaml.actions.internal

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.data.ActionData
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.EventCommonDataCommit
import com.tencent.devops.process.yaml.actions.internal.event.PipelineYamlManualEvent
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCred
import com.tencent.devops.process.yaml.git.pojo.PacGitPushResult
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitCred
import com.tencent.devops.process.yaml.git.service.PacGitApiService
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.YamlContent
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind

/**
 * 用户主动操作的action,如开启pac、发布流水线、删除流水线
 */
class PipelineYamlManualAction : BaseAction {
    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.MANUAL)
    override lateinit var data: ActionData
    fun event() = data.event as PipelineYamlManualEvent

    override lateinit var api: PacGitApiService

    override fun init(): BaseAction? {
        return initCommonData()
    }

    private fun initCommonData(): PipelineYamlManualAction {
        val event = event()
        val gitProjectId = getGitProjectIdOrName()
        val gitProjectInfo = api.getGitProjectInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId,
            retry = ApiRequestRetryInfo(true)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(gitProjectId)
        )
        val defaultBranch = gitProjectInfo.defaultBranch!!
        val latestCommit = api.getGitCommitInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId,
            sha = defaultBranch,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = EventCommonData(
            gitProjectId = gitProjectId,
            userId = event.userId,
            branch = defaultBranch,
            commit = EventCommonDataCommit(
                commitId = latestCommit?.commitId ?: "0",
                commitMsg = latestCommit?.commitMsg,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(latestCommit?.commitDate),
                commitAuthorName = latestCommit?.commitAuthor
            ),
            projectName = data.setting.projectName,
            scmType = event.scmType
        )
        this.data.context.defaultBranch = defaultBranch
        this.data.context.homePage = gitProjectInfo.homepage
        return this
    }

    override fun getGitProjectIdOrName(gitProjectId: String?) = gitProjectId ?: data.setting.projectName

    override fun getGitCred(personToken: String?): PacGitCred {
        val event = event()
        return when (event.scmType) {
            ScmType.CODE_GIT, ScmType.CODE_TGIT -> TGitCred(
                userId = event().authUserId,
                accessToken = personToken,
                useAccessToken = personToken == null
            )
            else -> TODO("对接其他代码库平台时需要补充")
        }
    }

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val homePage = data.context.homePage
        val commit = data.eventCommon.commit.commitId
        return GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = this.getGitProjectIdOrName(),
            ref = this.data.eventCommon.branch
        ).map { (name, blobId) ->
            val yamlUrl = "$homePage/blob/$commit/$name"
            YamlPathListEntry(
                yamlPath = name,
                checkType = CheckType.NEED_CHECK,
                ref = this.data.eventCommon.branch,
                blobId = blobId,
                yamlUrl = yamlUrl
            )
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

    override fun getChangeSet(): Set<String>? = null

    fun checkPushPermission(): Boolean {
        return api.checkPushPermission(
            userId = event().userId,
            cred = this.getGitCred(),
            gitProjectId = getGitProjectIdOrName(),
            authUserId = event().authUserId
        )
    }

    fun pushYamlFile(
        pipelineId: String,
        pipelineName: String,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction,
        versionName: String?,
        targetBranch: String?
    ): PacGitPushResult {
        return api.pushYamlFile(
            userId = event().authUserId,
            cred = this.getGitCred(),
            gitProjectId = getGitProjectIdOrName(),
            defaultBranch = data.eventCommon.branch,
            filePath = filePath,
            content = content,
            commitMessage = commitMessage,
            targetAction = targetAction,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            versionName = versionName,
            targetBranch = targetBranch
        )
    }
}
