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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIMrInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.MrCommentBody
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.enums.GitCodeApiStatus
import com.tencent.devops.stream.utils.RetryUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class StreamScmService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val oauthService: StreamOauthService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    @Lazy
    private val streamGitTokenService: StreamGitTokenService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamScmService::class.java)
        const val PROJECT_PERMISSION_ERROR = "[%s] No permissions"
    }

    // 获取工蜂超级token
    // 注意，应该优先从 stream gitToken获取token 如非必要，不应该调用这个
    @Throws(ErrorCodeException::class)
    fun getToken(
        gitProjectId: String
    ): GitToken {
        return retryFun(
            log = "$gitProjectId get token fail",
            apiErrorCode = ErrorCodeEnum.GET_TOKEN_ERROR,
            action = {
                client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!
            }
        )
    }

    @Throws(ErrorCodeException::class)
    fun refreshToken(projectId: String, refreshToken: String): GitToken {
        return retryFun(
            log = "$projectId refresh token fail",
            apiErrorCode = ErrorCodeEnum.REFRESH_TOKEN_ERROR,
            action = {
                client.getScm(ServiceGitCiResource::class).refreshToken(projectId, refreshToken).data!!
            }
        )
    }

    // 销毁工蜂超级token
    @Throws(ErrorCodeException::class)
    fun clearToken(
        gitProjectId: Long,
        token: String
    ): Boolean {
        return retryFun(
            log = "$gitProjectId clear token fail",
            apiErrorCode = ErrorCodeEnum.CLEAR_TOKEN_ERROR,
            action = {
                client.getScm(ServiceGitCiResource::class).clearToken(token).data ?: false
            }
        )
    }

    // 针对刚开始的获取项目信息获取超级token，遇到报错一定是项目不存在返回项目不存在信息
    fun getTokenForProject(
        gitProjectId: String
    ): GitToken? {
        try {
            return client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!
        } catch (e: Throwable) {
            when (e) {
                is ClientException -> {
                    error(
                        "getTokenForProject timeout ${e.message}",
                        ErrorCodeEnum.DEVNET_TIMEOUT_ERROR,
                        "get token from git time out"
                    )
                }
                is RemoteServiceException -> {
                    error(
                        "getTokenForProject git error ${e.message}",
                        ErrorCodeEnum.GET_TOKEN_ERROR,
                        ErrorCodeEnum.PROJECT_NOT_FOUND.formatErrorMessage.format(gitProjectId)
                    )
                }
                else -> {
                    error(
                        "getTokenForProject error ${e.message}",
                        ErrorCodeEnum.GET_TOKEN_ERROR,
                        ErrorCodeEnum.GET_TOKEN_ERROR.formatErrorMessage.format(e.message)
                    )
                }
            }
        }
        return null
    }

    fun getYamlFromGit(
        token: String,
        gitProjectId: String,
        fileName: String,
        ref: String,
        useAccessToken: Boolean,
        isFirst: Boolean = true
    ): String {
        logger.info("StreamScmService|getYamlFromGit|$gitProjectId|$fileName|$ref|$useAccessToken]")
        try {
            return retryFun(
                log = "$gitProjectId get yaml $fileName fail",
                apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getGitCIFileContent(
                        gitProjectId = gitProjectId,
                        filePath = fileName,
                        token = token,
                        ref = getTriggerBranch(ref),
                        useAccessToken = useAccessToken
                    ).data!!
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getYamlFromGit(newToken, gitProjectId, fileName, ref, useAccessToken, false)
            }
            throw e
        }
    }

    fun getProjectInfoRetry(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean,
        isFirst: Boolean = true
    ): GitCIProjectInfo {
        logger.info("StreamScmService|getProjectInfoRetry|gitProjectId|$gitProjectId")
        try {
            return retryFun(
                log = "$gitProjectId get project $gitProjectId fail",
                apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getProjectInfo(
                        accessToken = token,
                        gitProjectId = gitProjectId,
                        useAccessToken = useAccessToken
                    ).data!!
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectInfoRetry(newToken, gitProjectId, useAccessToken, false)
            }
            throw e
        }
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean,
        isFirst: Boolean = true
    ): GitCIProjectInfo? {
        logger.info("StreamScmService|getProjectInfo|gitProjectId|$gitProjectId|useAccessToken|$useAccessToken")
        try {
            val result = client.getScm(ServiceGitCiResource::class).getProjectInfo(
                accessToken = token,
                gitProjectId = gitProjectId,
                useAccessToken = useAccessToken
            )
            return result.data
        } catch (e: RemoteServiceException) {
            logger.warn(
                "StreamScmService|getProjectInfo|RemoteServiceException|" +
                    "${e.httpStatus}|${e.errorCode}|${e.errorMessage}|${e.responseContent}"
            )
            when (e.httpStatus) {
                GitCodeApiStatus.NOT_FOUND.status -> {
                    error(
                        "getProjectInfo error ${e.errorMessage}",
                        ErrorCodeEnum.PROJECT_NOT_FOUND,
                        ErrorCodeEnum.PROJECT_NOT_FOUND.formatErrorMessage.format(gitProjectId)
                    )
                }
                GitCodeApiStatus.FORBIDDEN.status -> {
                    error(
                        logMessage = "getProjectInfo error ${e.errorMessage}",
                        errorCode = ErrorCodeEnum.GET_PROJECT_INFO_FORBIDDEN
                    )
                }
                else -> {
                    error(
                        logMessage = "getProjectInfo error ${e.errorMessage}",
                        errorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR,
                        exceptionMessage = ErrorCodeEnum.GET_PROJECT_INFO_ERROR.formatErrorMessage
                            .format(gitProjectId, e.errorMessage)
                    )
                }
            }
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectInfo(newToken, gitProjectId, useAccessToken, false)
            }
            throw e
        } catch (e: Exception) {
            logger.warn("StreamScmService|getProjectInfo|Exception", e)
            error(" getProjectInfo error ${e.message}", ErrorCodeEnum.GET_PROJECT_INFO_ERROR)
        }
        return null
    }

    fun getCommits(
        token: String,
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        perPage: Int?,
        isFirst: Boolean = true
    ): List<Commit>? {
        logger.info("StreamScmService|getCommits|$gitProjectId|$filePath|$branch|$since|$until|$page|$perPage")
        try {
            return client.getScm(ServiceGitResource::class).getCommits(
                gitProjectId = gitProjectId,
                filePath = filePath,
                branch = branch,
                token = token,
                since = since,
                until = until,
                page = page ?: 1,
                perPage = perPage ?: 20,
                tokenType = TokenTypeEnum.OAUTH
            ).data
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getCommits(newToken, gitProjectId, filePath, branch, since, until, page, perPage, false)
            }
            throw e
        }
    }

    fun createNewFile(
        userId: String,
        token: String,
        gitProjectId: String,
        gitCICreateFile: GitCICreateFile,
        isFirst: Boolean = true
    ): Boolean {
        logger.info("StreamScmService|createNewFile|$gitProjectId|$gitCICreateFile")
        try {
            return client.getScm(ServiceGitResource::class).gitCICreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitCreateFile = GitCICreateFile(
                    filePath = gitCICreateFile.filePath,
                    branch = gitCICreateFile.branch,
                    encoding = gitCICreateFile.encoding,
                    content = gitCICreateFile.content,
                    commitMessage = gitCICreateFile.commitMessage
                )
            ).data!!
        } catch (e: RemoteServiceException) {
            logger.warn(
                "StreamScmService|createNewFile|RemoteServiceException|" +
                    "${e.httpStatus}|${e.errorCode}|${e.errorMessage}|${e.responseContent}"
            )
            if (GitCodeApiStatus.getStatus(e.httpStatus) != null) {
                error(
                    logMessage = "createNewFile error ${e.errorMessage}",
                    errorCode = ErrorCodeEnum.CREATE_NEW_FILE_GIT_API_ERROR,
                    exceptionMessage = ErrorCodeEnum.CREATE_NEW_FILE_GIT_API_ERROR.formatErrorMessage
                        .format(gitCICreateFile.filePath, gitCICreateFile.branch, e.httpStatus, e.errorMessage)
                )
            } else {
                error(
                    logMessage = "createNewFile error ${e.errorMessage}",
                    errorCode = ErrorCodeEnum.CREATE_NEW_FILE_ERROR,
                    exceptionMessage = ErrorCodeEnum.CREATE_NEW_FILE_ERROR.formatErrorMessage.format(e.errorMessage)
                )
            }
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return createNewFile(userId, newToken, gitProjectId, gitCICreateFile, false)
            }
            throw e
        } catch (e: Exception) {
            logger.warn("StreamScmService|createNewFile|Exception", e)
            error(" createNewFile error ${e.message}", ErrorCodeEnum.CREATE_NEW_FILE_ERROR)
        }
        return false
    }

    fun getProjectMembers(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        isFirst: Boolean = true
    ): List<GitMember>? {
        logger.info("StreamScmService|getProjectMembers|$gitProjectId|$page|$pageSize|$search")
        try {
            return client.getScm(ServiceGitCiResource::class).getMembers(
                token = token,
                gitProjectId = gitProjectId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                search = search
            ).data
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectMembers(newToken, gitProjectId, page, pageSize, search, false)
            }
            throw e
        }
    }

    fun getProjectMembersRetry(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        isFirst: Boolean = true
    ): List<GitMember>? {
        return try {
            retryFun(
                log = "getProjectMembersRetry: [$gitProjectId|$page|$pageSize|$search]",
                apiErrorCode = ErrorCodeEnum.GET_GIT_PROJECT_MEMBERS_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getMembers(
                        token = token,
                        gitProjectId = gitProjectId,
                        page = page ?: 1,
                        pageSize = pageSize ?: 20,
                        search = search
                    ).data
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectMembersRetry(newToken, gitProjectId, page, pageSize, search, false)
            }
            throw e
        }
    }

    fun getProjectBranchesRetry(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        isFirst: Boolean = true
    ): List<String>? {
        return try {
            retryFun(
                log = "getProjectBranchesRetry: [$gitProjectId] error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getBranches(
                        token = token,
                        gitProjectId = gitProjectId,
                        page = page ?: 1,
                        pageSize = pageSize ?: 20,
                        search = null,
                        orderBy = null,
                        sort = null
                    ).data
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectBranchesRetry(newToken, gitProjectId, page, pageSize, false)
            }
            throw e
        }
    }

    fun getProjectBranches(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?,
        isFirst: Boolean = true
    ): List<String>? {
        logger.info("StreamScmService|getProjectBranches|$gitProjectId|$page|$pageSize|$search|$orderBy|$sort")
        return try {
            client.getScm(ServiceGitCiResource::class)
                .getBranches(
                    token = token,
                    gitProjectId = gitProjectId,
                    page = page ?: 1,
                    pageSize = pageSize ?: 20,
                    search = search,
                    orderBy = orderBy,
                    sort = sort
                ).data
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getProjectBranches(newToken, gitProjectId, page, pageSize, search, orderBy, sort, false)
            }
            throw e
        }
    }

    // 获取项目ID，兼容没有source字段的旧数据，和fork库中源项目id不同的情况
    fun getProjectId(isFork: Boolean = false, gitRequestEventForHandle: GitRequestEventForHandle): Long {
        with(gitRequestEventForHandle) {
            return if (isFork && !checkRepoTrigger) {
                gitRequestEvent.sourceGitProjectId!!
            } else {
                gitProjectId
            }
        }
    }

    fun getMergeRequestChangeInfo(
        userId: String?,
        token: String?,
        gitProjectId: Long,
        mrId: Long,
        isFirst: Boolean = true
    ): GitMrChangeInfo? {
        logger.info("StreamScmService|getMergeRequestChangeInfo|$gitProjectId|$mrId")
        return try {
            retryFun(
                log = "$gitProjectId get mr $mrId changeInfo error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO,
                action = {
                    client.getScm(ServiceGitCiResource::class).getMergeRequestChangeInfo(
                        token = if (userId == null) {
                            token!!
                        } else {
                            getOauthToken(userId, true, gitProjectId)
                        },
                        gitProjectId = gitProjectId,
                        mrId = mrId
                    ).data
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getMergeRequestChangeInfo(userId, newToken, gitProjectId, mrId, false)
            }
            throw e
        }
    }

    fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeProjectInfo>? {
        logger.info("StreamScmService|getProjectList|$userId|$page|$pageSize|$search")
        return client.getScm(ServiceGitCiResource::class).getProjectList(
            accessToken = accessToken,
            userId = userId,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = orderBy, sort = sort, owned = owned, minAccessLevel = minAccessLevel
        ).data
    }

    fun getFileInfo(
        token: String,
        gitProjectId: String,
        filePath: String?,
        ref: String?,
        useAccessToken: Boolean,
        isFirst: Boolean = true
    ): GitCodeFileInfo? {
        logger.info("StreamScmService|getFileInfo|$gitProjectId|$filePath][$ref")
        return try {
            retryFun(
                log = "getFileInfo: [$gitProjectId|$filePath][$ref] error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getGitFileInfo(
                        gitProjectId = gitProjectId,
                        filePath = filePath,
                        ref = ref,
                        token = token,
                        useAccessToken = useAccessToken
                    ).data
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return getFileInfo(newToken, gitProjectId, filePath, ref, useAccessToken, false)
            }
            throw e
        }
    }

    fun getMergeInfo(
        gitProjectId: Long,
        mergeRequestId: Long,
        token: String,
        isFirst: Boolean = true
    ): GitCIMrInfo {
        logger.info("StreamScmService|getMergeInfo|$gitProjectId|$mergeRequestId")
        return try {
            retryFun(
                log = "$gitProjectId get mr $mergeRequestId info error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO,
                action = {
                    client.getScm(ServiceGitResource::class).getGitCIMrInfo(
                        gitProjectId = gitProjectId,
                        mergeRequestId = mergeRequestId,
                        token = token
                    ).data!!
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getMergeInfo(gitProjectId, mergeRequestId, newToken, false)
            }
            throw e
        }
    }

    fun getFileTreeFromGit(
        gitProjectId: Long,
        token: String,
        filePath: String,
        ref: String?,
        recursive: Boolean?,
        isFirst: Boolean = true
    ): List<GitFileInfo> {
        return try {
            retryFun(
                log = "$gitProjectId get $filePath file tree error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR,
                action = {
                    client.getScm(ServiceGitResource::class).getGitCIFileTree(
                        gitProjectId = gitProjectId.toString(),
                        path = filePath,
                        token = token,
                        ref = ref,
                        recursive = recursive,
                        TokenTypeEnum.OAUTH
                    ).data ?: emptyList()
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getFileTreeFromGit(gitProjectId, newToken, filePath, ref, false)
            }
            throw e
        }
    }

    // 获取指定文件在项目中的文件树信息，用于删除分支时判断yml是否在默认分支还存在的情况
    fun getFileTreeFromGitWithDefaultBranch(
        gitToken: String,
        gitProjectId: Long,
        filePath: String,
        recursive: Boolean?,
        isFirst: Boolean = true
    ): List<GitFileInfo> {
        return try {
            retryFun(
                log = "$gitProjectId get $filePath file tree error",
                apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR,
                action = {
                    client.getScm(ServiceGitResource::class).getGitCIFileTree(
                        gitProjectId = gitProjectId.toString(),
                        path = if (filePath.contains("/")) {
                            filePath.substring(0, filePath.lastIndexOf("/"))
                        } else {
                            filePath
                        },
                        token = gitToken,
                        ref = "",
                        recursive = recursive,
                        TokenTypeEnum.OAUTH
                    ).data ?: emptyList()
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getFileTreeFromGitWithDefaultBranch(newToken, gitProjectId, filePath, false)
            }
            throw e
        }
    }

    fun getCommitChangeFileListRetry(
        token: String?,
        userId: String?,
        gitProjectId: Long,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int,
        isFirst: Boolean = true
    ): List<ChangeFileInfo> {
        return try {
            retryFun(
                log = "getCommitChangeFileListRetry from: $from to: $to error",
                apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR,
                action = {
                    client.getScm(ServiceGitCiResource::class).getCommitChangeFileList(
                        token = if (userId == null) {
                            token!!
                        } else {
                            getOauthToken(userId, true, gitProjectId)
                        },
                        gitProjectId = gitProjectId.toString(),
                        from = from,
                        to = to,
                        straight = straight,
                        page = page,
                        pageSize = pageSize,
                        useAccessToken = true
                    ).data ?: emptyList()
                }
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId, true)
                return getCommitChangeFileListRetry(
                    newToken,
                    userId,
                    gitProjectId,
                    from,
                    to,
                    straight,
                    page,
                    pageSize,
                    false
                )
            }
            throw e
        }
    }

    fun getLatestRevisionRetry(
        pipelineId: String,
        gitToken: String,
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String,
        userName: String
    ): RevisionInfo? {
        return retryFun(
            log = "timer|[$pipelineId] get latestRevision fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR,
            action = {
                client.get(ServiceScmOauthResource::class).getLatestRevision(
                    projectName = projectName,
                    url = url,
                    type = ScmType.CODE_GIT,
                    branchName = branchName,
                    additionalPath = null,
                    privateKey = null,
                    passPhrase = null,
                    token = gitToken,
                    region = null,
                    userName = userName
                ).data
            }
        )
    }

    fun getCommitInfo(
        gitToken: String,
        projectName: String,
        sha: String
    ): GitCommit? {
        logger.info("StreamScmService|getCommitInfo|$projectName|$sha")
        return client.getScm(ServiceGitResource::class).getRepoRecentCommitInfo(
            repoName = projectName,
            sha = sha,
            token = gitToken,
            tokenType = TokenTypeEnum.OAUTH
        ).data
    }

    fun addMrComment(
        token: String,
        gitProjectId: String,
        mrId: Long,
        mrBody: MrCommentBody,
        isFirst: Boolean = true
    ) {
        logger.info("StreamScmService|addMrComment|$gitProjectId|$mrId")
        try {
            client.getScm(ServiceGitCiResource::class).addMrComment(
                token = token,
                gitProjectId = gitProjectId,
                mrId = mrId,
                mrBody = mrBody
            )
        } catch (e: CustomException) {
            if (e.status.statusCode == Response.Status.FORBIDDEN.statusCode && isFirst) {
                val newToken = streamGitTokenService.getToken(gitProjectId.toLong(), true)
                return addMrComment(newToken, gitProjectId, mrId, mrBody, false)
            }
            throw e
        }
    }

    fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeGroup>? {
        logger.info("StreamScmService|getProjectGroupList|$accessToken|$page|$pageSize")
        return client.getScm(ServiceGitCiResource::class).getProjectGroupsList(
            accessToken = accessToken,
            page = page,
            pageSize = pageSize,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data
    }

    fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    private fun getOauthToken(userId: String, isEnableUser: Boolean, gitProjectId: Long): String {
        return if (isEnableUser) {
            val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
            oauthService.getAndCheckOauthToken(setting!!.enableUserId).accessToken
        } else {
            return oauthService.getAndCheckOauthToken(userId).accessToken
        }
    }

    private fun <T> retryFun(log: String, apiErrorCode: ErrorCodeEnum, action: () -> T): T {
        try {
            return RetryUtils.clientRetry {
                action()
            }
        } catch (e: ClientException) {
            logger.warn("StreamScmService|retryFun|retry 5 times $log: ${e.message} ")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.formatErrorMessage
            )
        } catch (e: RemoteServiceException) {
            logger.warn("StreamScmService|retryFun|GIT_API_ERROR $log: ${e.message} ")
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("StreamScmService|retryFun|GIT_SCM_ERROR $log: ${e.message} ")
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.message}"
            )
        } catch (e: Throwable) {
            logger.warn("StreamScmService|retryFun|error|$log|${e.message} ")
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

    // 返回给前端错误码异常
    private fun error(logMessage: String, errorCode: ErrorCodeEnum, exceptionMessage: String? = null) {
        logger.warn("StreamScmService|error|$logMessage")
        throw ErrorCodeException(
            statusCode = 200,
            errorCode = errorCode.errorCode.toString(),
            defaultMessage = exceptionMessage ?: errorCode.formatErrorMessage
        )
    }
}
