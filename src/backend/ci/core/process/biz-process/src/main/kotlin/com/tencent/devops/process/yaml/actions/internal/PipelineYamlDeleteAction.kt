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

import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.GitBaseAction
import com.tencent.devops.process.yaml.actions.data.ActionData
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.service.PacGitApiService
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.YamlContent
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry

/**
 * 分支或tag删除对应的action
 */
class PipelineYamlDeleteAction(
    private val gitAction: GitBaseAction,
    private val pipelineYamlService: PipelineYamlService
) : BaseAction {

    override val metaData: ActionMetaData = gitAction.metaData
    override var data: ActionData = gitAction.data
    override val api: PacGitApiService = gitAction.api

    override fun init(): BaseAction? {
        gitAction.init()
        return this
    }

    override fun initCacheData() {
        gitAction.initCacheData()
    }

    override fun getGitProjectIdOrName(gitProjectId: String?) = gitAction.getGitProjectIdOrName(gitProjectId)

    override fun getGitCred(personToken: String?) = gitAction.getGitCred(personToken)

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val yamlPathList = mutableListOf<YamlPathListEntry>()
        val defaultBranchFiles = GitActionCommon.getYamlPathList(
            action = gitAction,
            gitProjectId = getGitProjectIdOrName(),
            ref = data.context.defaultBranch
        ).map { (name, blobId) ->
            YamlPathListEntry(name, CheckType.NO_NEED_CHECK, data.context.defaultBranch, blobId)
        }
        val deleteFileBranchFiles = pipelineYamlService.getAllBranchFilePath(
            projectId = gitAction.data.setting.projectId,
            repoHashId = gitAction.data.setting.repoHashId,
            branch = gitAction.data.eventCommon.branch
        ).map { filePath ->
            YamlPathListEntry(filePath, CheckType.NEED_DELETE, gitAction.data.eventCommon.branch, null)
        }
        yamlPathList.addAll(defaultBranchFiles)
        yamlPathList.addAll(deleteFileBranchFiles)
        return yamlPathList
    }

    override fun getYamlContent(fileName: String): YamlContent {
        return YamlContent(
            ref = data.context.defaultBranch!!,
            content = api.getFileContent(
                cred = gitAction.getGitCred(),
                gitProjectId = getGitProjectIdOrName(),
                fileName = fileName,
                ref = data.context.defaultBranch!!,
                retry = ApiRequestRetryInfo(true)
            )
        )
    }

    override fun getChangeSet(): Set<String>? = null
}
