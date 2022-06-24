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

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.github.request.GHGetBranchRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.api.github.ServiceGithubPRResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.api.github.ServiceGithubUserResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
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
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import com.tencent.devops.stream.util.QualityUtils
import com.tencent.devops.stream.util.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

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
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
        ) {
            // TODO: 2022/6/20 github获取项目信息接口
            client.get(ServiceGithubRepositoryResource::class).getRepository(
                request = GetRepositoryRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId
                ),
                userId = cred.getUserId()
            )
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
            retry = retry,
            log = "$gitProjectId get commit info $sha fail",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_INFO_ERROR
        ) {
            // TODO: 2022/6/20 github根据hash值、分支名或tag 获取提交信息
            client.get(ServiceGithubCommitsResource::class).getCommit(
                request = GetCommitRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    ref = sha
                ),
                userId = cred.getUserId()
            )
            // todo 注意信息是否正确
        }?.let { GithubCommitInfo(it) }
    }

    override fun getUserInfoByToken(cred: StreamGitCred): GithubUserInfo? {
        // TODO: 2022/6/20 github 根据token 获取用户信息
        return client.get(ServiceGithubUserResource::class).getUser(
            userId = cred.getUserId()
        ).let { GithubUserInfo(id = it.id.toString(), username = it.login) }
    }


    override fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): GithubProjectUserInfo {
        // TODO: 2022/6/20 github 获取某项目下的用户相关信息 => 主要是获取access level
        return client.get(ServiceGithubRepositoryResource::class).getRepositoryPermissions(
            request = GetRepositoryPermissionsRequest(
                owner = userId,
                repo = gitProjectId,
                username = userId
            ),
            userId = userId
        ).let {
            // todo 权限如何映射?
            GithubProjectUserInfo(it)
        }
    }

    override fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GithubMrInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
        ) {
            // TODO: 2022/6/20 github 根据prId 获取到pr信息.=> 主要是获取pr状态.
            client.get(ServiceGithubPRResource::class).getPullRequest(
                userId = cred.getUserId(),
                request = GetPullRequestRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    // todo mrId和 pullNumber注意是否一致
                    pullNumber = mrId
                )
            )
        }?.let {
            GithubMrInfo(
                mergeStatus = it.state,
                // todo 注意basecommit是否一致
                baseCommit = it.base.sha
            )
        }
    }


    // todo repository还未提供接口
    override fun getMrChangeInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): GithubMrChangeInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get mr $mrId changeInfo error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO
        ) {
            // TODO: 2022/6/20 github 获得对应prid 的变更文件列表 主要获取4个字段 => oldPath newPath 是否renameFile 是否deletedFile
            client.get(ServiceGithubPRResource::class).listPullRequestFiles(
                userId = cred.getUserId(),
                request = ListPullRequestFileRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    // todo mrId和 pullNumber注意是否一致
                    pullNumber = mrId
                )
            )
        }?.let {
            GithubMrChangeInfo(
                files = it.map { f ->
                    // todo 注意参数是否正确
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
            retry = retry,
            log = "$gitProjectId get $path file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR
        ) {
            // TODO: 2022/6/20 github 通过路径和分支去获取对应目录的文件列表 主要需要2字段 => 文件名 文件类型
            client.get(ServiceGithubDatabaseResource::class).getTree(
                userId = cred.getUserId(),
                request = GetTreeRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    // todo path 和ref组合在一起的tree_sha语法
                    // /repos/Florence-y/note/git/trees/main:
                    treeSha = "${ref ?: "master"}:${path ?: ""}"
                )
            )
        }.tree.map { GithubTreeFileInfo(it) }
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
            retry = retry,
            log = "$gitProjectId get yaml $fileName from $ref fail",
            apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR
        ) {
            // TODO: 2022/6/20 根据文件路径和分支名获取文件内容 => 得到文件内容
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    path = fileName,
                    ref = ref
                ),
                userId = cred.getUserId()
            ).content ?: ""
        }
    }

    private fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
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
            retry = retry,
            log = "getFileInfo: [$gitProjectId|$fileName][$ref] error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR
        ) {
            // TODO: 2022/6/20 github 得到 => 文件内容(base64) blobId
            client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
                request = GetRepositoryContentRequest(
                    owner = cred.getUserId(),
                    repo = gitProjectId,
                    path = fileName,
                    ref = ref ?: "master"
                ),
                userId = cred.getUserId()
            )
        }.let { GithubFileInfo(content = it.content ?: "", blobId = it.sha) }
    }

    override fun getProjectList(
        cred: StreamGitCred,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GithubProjectInfo>? {
        // TODO: 2022/6/20 获取用户(token)对应的项目列表
        // todo search、minAccessLevel参数现在不可用
        return client.get(ServiceGithubRepositoryResource::class).listRepositories(
            request = ListRepositoriesRequest(),
            userId = cred.getUserId()
        ).map {
            GithubProjectInfo(
                gitProjectId = it.gitProjectId.toString(),
                defaultBranch = it.defaultBranch,
                gitHttpUrl = it.gitHttpUrl ?: "",
                name = it.name ?: "",
                gitSshUrl = it.gitSshUrl,
                homepage = it.homepage,
                gitHttpsUrl = it.gitHttpUrl,
                description = it.description,
                avatarUrl = it.avatarUrl,
                pathWithNamespace = it.nameWithNamespace,
                nameWithNamespace = it.nameWithNamespace ?: ""
            )
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
        return doRetryFun(
            retry = retry,
            log = "timer|[$pipelineId] get latestRevision fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR
        ) {
            // TODO: 2022/6/20  getLatestRevision并没有对github的实现
            client.get(ServiceGithubBranchResource::class).getBranch(
                    request = GHGetBranchRequest(
                        owner = userName,
                        repo = projectName,
                        branch = branch
                    ),
                    userId = userName
            // todo 注意信息是否正确
                )?.let { GithubRevisionInfo(it) }
        }
    }


    // 以下非StreamGitApiService接口实现
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
    ): List<GithubChangeFileInfo> {
        return doRetryFun(
            retry = retry,
            log = "getCommitChangeFileListRetry from: $from to: $to error",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR
        ) {
            // TODO: 2022/6/20 获取两个commit之间的差异文件
            client.get(ServiceGitResource::class).getChangeFileList(
                cred.toToken(),
                cred.toTokenType(),
                gitProjectId = gitProjectId,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            ).data ?: emptyList()
        }.map { GithubChangeFileInfo(it) }
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
        // TODO: 2022/6/20 github版添加评论注意mrbody的问题
        return client.get(ServiceGitResource::class).addMrComment(
            token = cred.toToken(),
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = QualityUtils.getQualityReport(mrBody.reportData.first, mrBody.reportData.second),
            tokenType = cred.toTokenType()
        )
    }

    protected fun StreamGitCred.toToken(): String {
        this as GithubCred
        if (this.accessToken != null) {
            return this.accessToken
        }
        return client.get(ServiceOauthResource::class).gitGet(this.userId!!).data?.accessToken
            ?: throw CustomException(
                Response.Status.FORBIDDEN,
                "STEAM PROJECT ENABLE USER NO OAUTH PERMISSION"
            )
    }

    protected fun StreamGitCred.toTokenType(): TokenTypeEnum {
        this as GithubCred
        return if (this.useAccessToken) {
            TokenTypeEnum.OAUTH
        } else {
            TokenTypeEnum.PRIVATE_KEY
        }
    }

    protected fun StreamGitCred.getUserId(): String {
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

    protected fun <T> doRetryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        return if (retry.retry) {
            retryFun(
                retry = retry,
                log = log,
                apiErrorCode = apiErrorCode
            ) {
                action()
            }
        } else {
            action()
        }
    }

    private fun <T> retryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        try {
            return RetryUtils.clientRetry(
                retry.retryTimes,
                retry.retryPeriodMills
            ) {
                action()
            }
        } catch (e: ClientException) {
            logger.warn("retry 5 times $log", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.formatErrorMessage
            )
        } catch (e: RemoteServiceException) {
            logger.warn("GIT_API_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("GIT_SCM_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.message}"
            )
        } catch (e: Throwable) {
            logger.error("retryFun error $log", e)
            throw ErrorCodeException(
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = if (e.message.isNullOrBlank()) {
                    "$log: ${apiErrorCode.formatErrorMessage}"
                } else {
                    "$log: ${e.message}"
                }
            )
        }
    }
}
