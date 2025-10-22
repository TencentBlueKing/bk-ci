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
 */

package com.tencent.devops.process.yaml.actions.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.GitBaseAction
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.EventCommonDataCommit
import com.tencent.devops.process.yaml.actions.data.PacRepoSetting
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.service.TGitApiService
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.YamlContent
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TGitNoteActionGit @Autowired constructor(
    private val apiService: TGitApiService,
    private val client: Client
) : TGitActionGit(apiService), GitBaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitNoteActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(StreamObjectKind.NOTE)

    override fun event() = data.event as GitNoteEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction? {
        if (data.isSettingInitialized) {
            return initCommonData()
        }
        val externalId = event().projectId.toString()
        val repository = client.get(ServiceRepositoryPacResource::class).getPacRepository(
            externalId = externalId, scmType = ScmType.CODE_GIT
        ).data ?: run {
            logger.info("pipeline yaml tgit review action init|repository not enable pac|$externalId")
            return null
        }
        data.setting = PacRepoSetting(repository = repository)
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val gitProjectId = event.projectId.toString()

        val gitProjectInfo = apiService.getGitProjectInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId,
            retry = ApiRequestRetryInfo(true)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(gitProjectId)
        )
        val defaultBranch = gitProjectInfo.defaultBranch!!
        val latestCommit = apiService.getGitCommitInfo(
            cred = this.getGitCred(),
            gitProjectId = gitProjectId,
            sha = defaultBranch,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.objectAttributes.projectId.toString(),
            scmType = ScmType.CODE_GIT,
            branch = defaultBranch,
            commit = EventCommonDataCommit(
                commitId = latestCommit?.commitId ?: "0",
                commitMsg = event.objectAttributes.note,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(latestCommit?.commitDate),
                commitAuthorName = latestCommit?.commitAuthor
            ),
            userId = event.user.username,
            projectName = GitUtils.getProjectName(event.repository.homepage)
        )
        return this
    }

    override fun initCacheData() {
        val event = event()
        if (data.isSettingInitialized && event.mergeRequest != null) {
            try {
                data.context.gitMrInfo = apiService.getMrInfo(
                    cred = getGitCred(),
                    gitProjectId = data.eventCommon.gitProjectId,
                    mrId = event.mergeRequest!!.id.toString(),
                    retry = ApiRequestRetryInfo(true)
                )?.baseInfo
                data.context.gitMrReviewInfo = apiService.getMrReview(
                    cred = getGitCred(),
                    gitProjectId = event.mergeRequest!!.target_project_id.toString(),
                    mrId = event.mergeRequest!!.id.toString(),
                    retry = ApiRequestRetryInfo(true)
                )
            } catch (ignore: Throwable) {
                logger.warn("TGit note action cache mrInfo/mrReviewInfo error", ignore)
            }
        }
    }

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
}
