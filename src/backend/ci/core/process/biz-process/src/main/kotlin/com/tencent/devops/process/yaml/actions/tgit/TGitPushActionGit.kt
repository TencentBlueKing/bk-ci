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

package com.tencent.devops.process.yaml.actions.tgit
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.GitBaseAction
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.EventCommonDataCommit
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.service.TGitApiService
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.YamlContent
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@Suppress("ALL")
class TGitPushActionGit(
    private val apiService: TGitApiService
) : TGitActionGit(apiService), GitBaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitPushActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.PUSH)

    override fun event() = data.event as GitPushEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction? {
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val lastCommit = getLatestCommit(event)
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.project_id.toString(),
            scmType = ScmType.CODE_GIT,
            branch = event.ref.removePrefix("refs/heads/"),
            commit = EventCommonDataCommit(
                commitId = event.after,
                commitMsg = lastCommit?.message,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(lastCommit?.timestamp),
                commitAuthorName = lastCommit?.author?.name
            ),
            userId = event.user_name,
            projectName = GitUtils.getProjectName(event.repository.homepage)
        )
        return this
    }

    private fun getLatestCommit(
        event: GitPushEvent
    ): GitCommit? {
        if (event.isDeleteEvent()) {
            return null
        }
        val commitId = event.after
        val commits = event.commits
        commits?.forEach {
            if (it.id == commitId) {
                return it
            }
        }
        return null
    }

    override fun initCacheData() {
        val gitProjectId = event().project_id.toString()
        val gitProjectInfo = apiService.getGitProjectInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId,
            retry = ApiRequestRetryInfo(true)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(gitProjectId)
        )
        data.context.defaultBranch = gitProjectInfo.defaultBranch!!
    }

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val changeSet = getChangeSet()
        val changeYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = this.getGitProjectIdOrName(),
            ref = this.data.eventCommon.branch
        ).map { (name, blobId) ->
            YamlPathListEntry(
                yamlPath = name,
                checkType = if (changeSet?.contains(name) == true) {
                    CheckType.NEED_CHECK
                } else {
                    CheckType.NO_NEED_CHECK
                },
                ref = this.data.eventCommon.branch, blobId = blobId
            )
        }.toMutableList()
        if (!data.context.deleteCiSet.isNullOrEmpty()) {
            val deleteYamlPathList = data.context.deleteCiSet?.map {
                YamlPathListEntry(
                    yamlPath = it,
                    checkType = CheckType.NEED_DELETE,
                    ref = this.data.eventCommon.branch, blobId = null
                )
            } ?: emptyList()
            changeYamlPathList.addAll(deleteYamlPathList)
        }
        return changeYamlPathList
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
        val changeFileList = mutableSetOf<String>()
        val deleteCiFileList = mutableSetOf<String>()
        event().diffFiles?.forEach {
            when {
                // 删除文件
                it.deletedFile -> {
                    changeFileList.add(it.oldPath)
                    if (GitActionCommon.isCiFile(it.oldPath)) {
                        deleteCiFileList.add(it.oldPath)
                    }
                }
                // 重命名文件
                it.renamedFile -> {
                    changeFileList.add(it.newPath)
                    changeFileList.add(it.oldPath)
                }
                // 修改或添加文件
                else -> changeFileList.add(it.newPath)
            }
        }
        data.context.changeSet = changeFileList
        data.context.deleteCiSet = deleteCiFileList
        return changeFileList
    }
}
