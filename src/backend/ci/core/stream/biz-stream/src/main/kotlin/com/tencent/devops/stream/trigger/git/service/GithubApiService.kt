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
import com.tencent.devops.common.sdk.github.request.CompareTwoCommitsRequest
import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.CreateIssueCommentRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.github.ServiceGithubCheckResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.api.github.ServiceGithubIssuesResource
import com.tencent.devops.repository.api.github.ServiceGithubPRResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.api.github.ServiceGithubUserResource
import com.tencent.devops.repository.pojo.enums.GithubAccessLevelEnum
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_COMPLETED
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.github.GitHubMrStatus
import com.tencent.devops.stream.trigger.git.pojo.github.GithubChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCommitDiffInfo
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
import com.tencent.devops.stream.trigger.git.service.StreamApiUtil.doRetryFun
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import com.tencent.devops.stream.util.QualityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("TooManyFunctions")
class GithubApiService @Autowired constructor(
    private val client: Client
) : StreamGitApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubApiService::class.java)
    }

    // github 组织白名单列表
    @Value("\${github.orgWhite:}")
    private var githubOrgWhite: String = ""

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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepository(
                request = GetRepositoryRequest(
                    repoName = gitProjectId
                ),
                token = cred.toToken()
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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get commit info $sha fail",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_INFO_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).getCommit(
                request = GetCommitRequest(
                    repoName = gitProjectId,
                    ref = sha
                ),
                token = cred.toToken()
            ).data
        }?.let { GithubCommitInfo(it) }
    }

    override fun getProjectMember(
        cred: StreamGitCred,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GithubProjectUserInfo> {
        return if (!search.isNullOrBlank()) {
            listOf(getProjectUserInfo(cred, search, gitProjectId))
        } else {
            client.get(ServiceGithubRepositoryResource::class).listRepositoryCollaborators(
                request = ListRepositoryCollaboratorsRequest(
                    repoName = gitProjectId,
                    page = page ?: 1,
                    perPage = pageSize ?: 30
                ),
                token = cred.toToken()
            ).data?.map {
                GithubProjectUserInfo(GithubAccessLevelEnum.getGithubAccessLevel(it.roleName).level, it.login)
            } ?: emptyList()
        }
    }

    override fun getUserInfoByToken(cred: StreamGitCred): GithubUserInfo? {
        return client.get(ServiceGithubUserResource::class).getUser(
            token = cred.toToken()
        ).data?.let { GithubUserInfo(id = it.id.toString(), username = it.login) }
    }

    override fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): GithubProjectUserInfo {
        return client.get(ServiceGithubRepositoryResource::class).getRepositoryPermissions(
            request = GetRepositoryPermissionsRequest(
                repoName = gitProjectId,
                username = userId
            ),
            token = cred.toToken()
        ).data?.let {
            GithubProjectUserInfo(GithubAccessLevelEnum.getGithubAccessLevel(it.roleName).level, it.user.login)
        } ?: GithubProjectUserInfo(GithubAccessLevelEnum.GUEST.level, "no_user")
    }

    override fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GithubMrInfo? {
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
        ) {
            client.get(ServiceGithubPRResource::class).getPullRequest(
                token = cred.toToken(),
                request = GetPullRequestRequest(
                    repoName = gitProjectId,
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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get mr $mrId changeInfo error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO
        ) {
            client.get(ServiceGithubPRResource::class).listPullRequestFiles(
                token = cred.toToken(),
                request = ListPullRequestFileRequest(
                    repoName = gitProjectId,
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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get $path file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR
        ) {
            client.get(ServiceGithubDatabaseResource::class).getTree(
                token = cred.toToken(),
                request = GetTreeRequest(
                    repoName = gitProjectId,
                    treeSha = "${ref!!}:$path",
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
        cred as GithubCred
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "$gitProjectId get yaml $fileName from $ref fail",
            apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    repoName = gitProjectId,
                    path = fileName,
                    ref = ref
                ),
                token = cred.toToken()
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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "getFileInfo: [$gitProjectId|$fileName][$ref] error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR
        ) {
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    repoName = gitProjectId,
                    path = fileName,
                    ref = ref!!
                ),
                token = cred.toToken()
            )
        }.data?.let { GithubFileInfo(content = it.content ?: "", blobId = it.sha) }
    }

    override fun getProjectList(
        cred: StreamGitCred,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GithubProjectInfo> {
        // todo 跨库触发不支持
        return emptyList()
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
        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "timer|[$pipelineId] get latestRevision fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).getCommit(
                token = GithubCred(userId = enableUserId).toToken(),
                request = GetCommitRequest(
                    repoName = projectName,
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
        client.get(ServiceGithubIssuesResource::class).createIssueComment(
            token = cred.toToken(),
            request = CreateIssueCommentRequest(
                repoName = gitProjectId,
                issueNumber = mrId,
                body = QualityUtils.getQualityReport(mrBody.reportData.first, mrBody.reportData.second)
            )
        )
    }

    override fun addCommitCheck(request: CommitCheckRequest, retry: ApiRequestRetryInfo) {
        with(request) {
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).format(
                DateTimeFormatter.ISO_INSTANT
            )
            if (state == GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS) {
                client.get(ServiceGithubCheckResource::class).createCheckRunByToken(
                    token = token!!,
                    request = CreateCheckRunRequest(
                        repoName = projectName,
                        name = context,
                        headSha = commitId,
                        detailsUrl = targetUrl,
                        status = GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS,
                        startedAt = now
                    )
                )
            } else {
                val status = if (block) GITHUB_CHECK_RUNS_STATUS_COMPLETED else GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
                client.get(ServiceGithubCheckResource::class).createCheckRunByToken(
                    token = token!!,
                    request = CreateCheckRunRequest(
                        repoName = projectName,
                        name = context,
                        headSha = commitId,
                        detailsUrl = targetUrl,
                        status = status,
                        completedAt = now,
                        conclusion = state
                    )
                )
            }
        }
    }

    override fun getCommitDiff(
        cred: StreamGitCred,
        gitProjectId: String,
        sha: String
    ): List<GithubCommitDiffInfo> {
        return doRetryFun(
            logger = logger,
            retry = ApiRequestRetryInfo(),
            log = "getCommitDiff $gitProjectId $sha fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).getCommit(
                token = cred.toToken(),
                request = GetCommitRequest(
                    repoName = gitProjectId,
                    ref = sha
                )
            )
        }.data?.files?.map {
            GithubCommitDiffInfo(
                oldPath = it.filename,
                newPath = it.filename
            )
        } ?: emptyList()
    }

    // 以下非StreamGitApiService接口实现
    /**
     * 获取两个commit之间的差异文件
     * @param from 旧commit
     * @param to 新commit
     * @param straight true：两个点比较差异，false：三个点比较差异。默认是 false
     */
    @SuppressWarnings("LongParameterList")
    fun getCommitChangeList(
        cred: GithubCred,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean,
        page: Int,
        pageSize: Int,
        retry: ApiRequestRetryInfo
    ): List<GithubChangeFileInfo> {

        return doRetryFun(
            logger = logger,
            retry = retry,
            log = "getCommitChangeFileListRetry from: $from to: $to error",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR
        ) {
            client.get(ServiceGithubCommitsResource::class).compareTwoCommits(
                token = cred.toToken(),
                request = CompareTwoCommitsRequest(
                    repoName = gitProjectId,
                    base = from,
                    head = to,
                    page = page,
                    perPage = pageSize
                )
            ).data?.files ?: emptyList()
        }.map { GithubChangeFileInfo(it) }
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
