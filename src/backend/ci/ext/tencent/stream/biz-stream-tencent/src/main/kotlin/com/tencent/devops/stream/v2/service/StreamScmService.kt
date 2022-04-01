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

package com.tencent.devops.stream.v2.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitMember
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
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.MrCommentBody
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.enums.GitCodeApiStatus
import com.tencent.devops.stream.utils.RetryUtils
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamScmService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val oauthService: StreamOauthService,
    private val streamBasicSettingDao: StreamBasicSettingDao
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
        useAccessToken: Boolean
    ): String {
        logger.info("getYamlFromGit: [$gitProjectId|$fileName|$ref|$useAccessToken]")
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
    }

    fun getProjectInfoRetry(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo {
        logger.info("getProjectInfoRetry: [$gitProjectId]")
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
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        logger.info("GitCIProjectInfo: [$gitProjectId|$useAccessToken]")
        try {
            val result = client.getScm(ServiceGitCiResource::class).getProjectInfo(
                accessToken = token,
                gitProjectId = gitProjectId,
                useAccessToken = useAccessToken
            )
            return result.data
        } catch (e: RemoteServiceException) {
            logger.warn(
                "getProjectInfo RemoteServiceException|" +
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
        } catch (e: Exception) {
            logger.error("getProjectInfo Exception: $e")
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
        perPage: Int?
    ): List<Commit>? {
        logger.info("getCommits: [$gitProjectId|$filePath|$branch|$since|$until|$page|$perPage]")
        return client.getScm(ServiceGitResource::class).getCommits(
            gitProjectId = gitProjectId,
            filePath = filePath,
            branch = branch,
            token = token,
            since = since,
            until = until,
            page = page ?: 1,
            perPage = perPage ?: 20
        ).data
    }

    fun createNewFile(
        userId: String,
        token: String,
        gitProjectId: String,
        gitCICreateFile: GitCICreateFile
    ): Boolean {
        logger.info("createNewFile: [$gitProjectId|$gitCICreateFile]")
        try {
            return client.getScm(ServiceGitResource::class).gitCICreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitCICreateFile = gitCICreateFile
            ).data!!
        } catch (e: RemoteServiceException) {
            logger.warn(
                "createNewFile RemoteServiceException|" +
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
        } catch (e: Exception) {
            logger.error("createNewFile Exception: $e")
            error(" createNewFile error ${e.message}", ErrorCodeEnum.CREATE_NEW_FILE_ERROR)
        }
        return false
    }

    fun getProjectMembers(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitMember>? {
        logger.info("getProjectMembers: [$gitProjectId|$page|$pageSize|$search]")
        return client.getScm(ServiceGitCiResource::class).getMembers(
            token = token,
            gitProjectId = gitProjectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            search = search
        ).data
    }

    fun getProjectMembersRetry(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitMember>? {
        return retryFun(
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
    }

    fun getProjectBranchesRetry(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?
    ): List<String>? {
        return retryFun(
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
    }

    fun getProjectBranches(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): List<String>? {
        logger.info("getProjectBranches: [$gitProjectId|$page|$pageSize|$search|$orderBy|$sort]")
        return client.getScm(ServiceGitCiResource::class)
            .getBranches(
                token = token,
                gitProjectId = gitProjectId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                search = search,
                orderBy = orderBy,
                sort = sort
            ).data
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
        mrId: Long
    ): GitMrChangeInfo? {
        logger.info("getMergeRequestChangeInfo: [$gitProjectId|$mrId]")
        return retryFun(
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
        logger.info("getProjectList: [$userId|$page|$pageSize|$search]")
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
        useAccessToken: Boolean
    ): GitCodeFileInfo? {
        logger.info("getFileInfo: [$gitProjectId|$filePath][$ref]")
        return retryFun(
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
    }

    fun getMergeInfo(
        gitProjectId: Long,
        mergeRequestId: Long,
        token: String
    ): GitCIMrInfo {
        logger.info("getMergeInfo: [$gitProjectId|$mergeRequestId]")
        return retryFun(
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
    }

    fun getFileTreeFromGit(
        gitProjectId: Long,
        token: String,
        filePath: String,
        ref: String?
    ): List<GitFileInfo> {
        return retryFun(
            log = "$gitProjectId get $filePath file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR,
            action = {
                client.getScm(ServiceGitResource::class).getGitCIFileTree(
                    gitProjectId = gitProjectId,
                    path = filePath,
                    token = token,
                    ref = ref
                ).data ?: emptyList()
            }
        )
    }

    // 获取指定文件在项目中的文件树信息，用于删除分支时判断yml是否在默认分支还存在的情况
    fun getFileTreeFromGitWithDefaultBranch(
        gitToken: String,
        gitProjectId: Long,
        filePath: String
    ): List<GitFileInfo> {
        return retryFun(
            log = "$gitProjectId get $filePath file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR,
            action = {
                client.getScm(ServiceGitResource::class).getGitCIFileTree(
                    gitProjectId = gitProjectId,
                    path = if (filePath.contains("/")) {
                        filePath.substring(0, filePath.lastIndexOf("/"))
                    } else {
                        filePath
                    },
                    token = gitToken,
                    ref = ""
                ).data ?: emptyList()
            }
        )
    }

    fun getCommitChangeFileListRetry(
        token: String?,
        userId: String?,
        gitProjectId: Long,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int
    ): List<ChangeFileInfo> {
        return retryFun(
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
                    pageSize = pageSize
                ).data ?: emptyList()
            }
        )
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
        logger.info("getCommitInfo: [$projectName|$sha]")
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
        mrBody: MrCommentBody
    ) {
        logger.info("addMrComment: [$gitProjectId|$mrId]")
        client.getScm(ServiceGitCiResource::class).addMrComment(
            token = token,
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody
        )
    }

    fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GitCodeGroup>? {
        logger.info("getProjectGroupList: [$accessToken|$page|$pageSize]")
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
            logger.warn("retry 5 times $log: ${e.message} ")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.formatErrorMessage
            )
        } catch (e: RemoteServiceException) {
            logger.warn("GIT_API_ERROR $log: ${e.message} ")
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: Throwable) {
            logger.error("retryFun error $log: ${e.message} ")
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
        logger.warn(logMessage)
        throw ErrorCodeException(
            statusCode = 200,
            errorCode = errorCode.errorCode.toString(),
            defaultMessage = exceptionMessage ?: errorCode.formatErrorMessage
        )
    }
}
