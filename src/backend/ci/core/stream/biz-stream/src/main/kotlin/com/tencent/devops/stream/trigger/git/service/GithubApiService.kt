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

package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.api.github.ServiceGithubPRResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.api.github.ServiceGithubUserResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.GithubAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.github.GitHubMrStatus
import com.tencent.devops.stream.trigger.git.pojo.github.GithubChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCommitInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.pojo.github.GithubFileInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubMrChangeInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubMrInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubProjectInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubProjectUserInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubRevisionInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubTreeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubUserInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.stream.trigger.git.service.StreamApiUtil.doRetryFun
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import com.tencent.devops.stream.util.QualityUtils
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GithubApiService @Autowired constructor(
    private val client: Client
) : StreamGitApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubApiService::class.java)
    }

    /**
     * 通过凭据获取可以直接使用的token
     */
    override fun getToken(
        cred: StreamGitCred
    ): String {
        return cred.toToken()
    }

    override fun getGitProjectInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): GithubProjectInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepository(
                request = GetRepositoryRequest(
                    owner = owner,
                    repo = repo
                ),
                userId = cred.getUserId()
            ).data
        }?.let {
            GithubProjectInfo(it)
        }
    }

    override fun getGitCommitInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): GithubCommitInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get commit info $sha fail",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_INFO_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).getCommit(
                request = GetCommitRequest(
                    owner = owner,
                    repo = repo,
                    ref = sha
                ),
                userId = cred.getUserId()
            ).data
        }?.let { GithubCommitInfo(it) }
    }

    override fun getUserInfoByToken(cred: StreamGitCred): GithubUserInfo? {
        return client.get(ServiceGithubUserResource::class).getUser(
            userId = cred.getUserId()
        ).data?.let { GithubUserInfo(id = it.id.toString(), username = it.login) }
    }

    override fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): GithubProjectUserInfo {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return client.get(ServiceGithubRepositoryResource::class).getRepositoryPermissions(
            request = GetRepositoryPermissionsRequest(
                owner = owner,
                repo = repo,
                username = userId
            ),
            userId = userId
        ).data?.let {
            GithubProjectUserInfo(GithubAccessLevelEnum.getGithubAccessLevel(it.permission).level)
        } ?: GithubProjectUserInfo(GithubAccessLevelEnum.GUEST.level)
    }

    override fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GithubMrInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
        ) {
            client.get(ServiceGithubPRResource::class).getPullRequest(
                userId = cred.getUserId(),
                request = GetPullRequestRequest(
                    owner = owner,
                    repo = repo,
                    pullNumber = mrId
                )
            )
        }.data?.let {
            GithubMrInfo(
                mergeStatus = GitHubMrStatus.convertTGitMrStatus(it.mergeableState).value,
                baseCommit = it.base.sha
            )
        }
    }

    override fun getMrChangeInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GithubMrChangeInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId changeInfo error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO
        ) {
            client.get(ServiceGithubPRResource::class).listPullRequestFiles(
                userId = cred.getUserId(),
                request = ListPullRequestFileRequest(
                    owner = owner,
                    repo = repo,
                    pullNumber = mrId
                )
            )
        }.data?.let {
            GithubMrChangeInfo(
                files = it.map { f ->
                    GithubChangeFileInfo(f)
                }
            )
        }
    }

    override fun getFileTree(
        cred: StreamGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<GithubTreeFileInfo> {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get $path file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR
        ) {
            // TODO 无法使用path搜索
            client.get(ServiceGithubDatabaseResource::class).getTree(
                userId = cred.getUserId(),
                request = GetTreeRequest(
                    owner = owner,
                    repo = repo,
                    treeSha = ref!!,
                    recursive = recursive.toString()
                )
            )
        }.data?.tree?.map { GithubTreeFileInfo(it) } ?: emptyList()
    }

    override fun getFileContent(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String,
        retry: ApiRequestRetryInfo
    ): String {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        cred as GithubCred
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get yaml $fileName from $ref fail",
            apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    owner = owner,
                    repo = repo,
                    path = fileName,
                    ref = ref
                ),
                userId = cred.getUserId()
            ).data?.getDecodedContentAsString() ?: ""
        }
    }

    override fun getFileInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): GithubFileInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(gitProjectId)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "getFileInfo: [$gitProjectId|$fileName][$ref] error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    owner = owner,
                    repo = repo,
                    path = fileName,
                    ref = ref!!
                ),
                userId = cred.getUserId()
            )
        }.data?.let { GithubFileInfo(content = it.content ?: "", blobId = it.sha) }
    }

    override fun getProjectList(
        cred: StreamGitCred,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GithubProjectInfo> {
        // todo search、minAccessLevel参数现在不可用
        return client.get(ServiceGithubRepositoryResource::class).listRepositories(
            request = ListRepositoriesRequest(),
            userId = cred.getUserId()
        ).data!!.map {
            GithubProjectInfo(it)
        }
    }

    override fun getLatestRevision(
        pipelineId: String,
        projectName: String,
        gitUrl: String,
        branch: String,
        userName: String,
        enableUserId: String,
        retry: ApiRequestRetryInfo
    ): GithubRevisionInfo? {
        val (owner, repo) = GitUtils.getRepoGroupAndName(projectName)
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "timer|[$pipelineId] get latestRevision fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).getCommit(
                userId = userName,
                request = GetCommitRequest(
                    owner = owner,
                    repo = repo,
                    ref = branch
                )
            )
        }.data?.let {
            GithubRevisionInfo(
                revision = it.sha,
                updatedMessage = it.commit.committer.date,
                branchName = branch,
                authorName = it.commit.author.name
            )
        }
    }

    /**
     * 为mr添加评论
     */
    fun addMrComment(
        cred: GithubCred,
        gitProjectId: String,
        mrId: Long,
        mrBody: MrCommentBody
    ) {
        // 暂时无法兼容，服务间调用 mrBody 字符串转义存在问题
        return client.get(ServiceGitResource::class).addMrComment(
            token = cred.toToken(),
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = QualityUtils.getQualityReport(mrBody.reportData.first, mrBody.reportData.second),
            tokenType = TokenTypeEnum.OAUTH
        )
    }

    /**
     * 获取两个commit之间的差异文件
     * @param from 旧commit
     * @param to 新commit
     * @param straight true：两个点比较差异，false：三个点比较差异。默认是 false
     */
    fun getCommitChangeList(
        cred: GithubCred,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean,
        page: Int,
        pageSize: Int,
        retry: ApiRequestRetryInfo
    ): List<TGitChangeFileInfo> {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "getCommitChangeFileListRetry from: $from to: $to error",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR
        ) {
            client.get(ServiceGitResource::class).getChangeFileList(
                cred.toToken(),
                TokenTypeEnum.OAUTH,
                gitProjectId = gitProjectId,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            ).data ?: emptyList()
        }.map { TGitChangeFileInfo(it) }
    }

    private fun StreamGitCred.toToken(): String {
        this as GithubCred
        if (this.accessToken != null) {
            return this.accessToken
        }
        return client.get(ServiceGithubResource::class).getAccessToken(this.userId!!).data?.accessToken
            ?: throw CustomException(
                Response.Status.FORBIDDEN,
                "STEAM PROJECT ENABLE USER NO OAUTH PERMISSION"
            )
    }

    private fun StreamGitCred.getUserId(): String {
        this as GithubCred
        return if (!this.userId.isNullOrBlank()) {
            userId
        } else {
            throw CustomException(
                Response.Status.FORBIDDEN,
                "STEAM PROJECT ENABLE USER NO OAUTH PERMISSION"
            )
        }
    }
}
