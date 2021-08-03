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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.GitCodeApiStatus
import com.tencent.devops.gitci.utils.RetryUtils
import com.tencent.devops.gitci.v2.exception.ErrorCodeEnum
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIMrInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmService @Autowired constructor(
    private val client: Client,
    private val oauthService: OauthService,
    private val gitCIBasicSettingService: GitCIBasicSettingService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
        const val PROJECT_PERMISSION_ERROR = "[%s] No permissions"
    }

    // 获取工蜂超级token
    fun getToken(
        gitProjectId: String
    ): GitToken {
        return retryFun(
            log = "$gitProjectId get token fail",
            action = {
                client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!
            }
        )
    }

    fun getYamlFromGit(
        token: String,
        gitProjectId: String,
        fileName: String,
        ref: String,
        useAccessToken: Boolean
    ): String {
        logger.info("getYamlFromGit: [$gitProjectId|$fileName|$token|$ref|$useAccessToken]")
        val result = retryFun(
            log = "$gitProjectId get yaml $fileName fail",
            action = {
                client.getScm(ServiceGitCiResource::class).getGitCIFileContent(
                    gitProjectId = gitProjectId,
                    filePath = fileName,
                    token = token,
                    ref = getTriggerBranch(ref),
                    useAccessToken = useAccessToken
                )
            }
        )
        if (result.data == null) {
            throw RuntimeException("yaml $fileName is null")
        }
        return result.data!!
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        logger.info("GitCIProjectInfo: [$gitProjectId|$token|$useAccessToken]")
        try {
            val result = client.getScm(ServiceGitCiResource::class).getProjectInfo(
                accessToken = token,
                gitProjectId = gitProjectId,
                useAccessToken = useAccessToken
            )
            return result.data
        } catch (e: RemoteServiceException) {
            logger.error("getProjectInfo RemoteServiceException|" +
                "${e.httpStatus}|${e.errorCode}|${e.errorMessage}|${e.responseContent}")
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
        logger.info("getCommits: [$gitProjectId|$filePath|$branch|$token|$since|$until|$page|$perPage]")
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
        logger.info("createNewFile: [$gitProjectId|$token|$gitCICreateFile]")
        try {
            return client.getScm(ServiceGitResource::class).gitCICreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitCICreateFile = gitCICreateFile
            ).data!!
        } catch (e: RemoteServiceException) {
            logger.error("createNewFile RemoteServiceException|" +
                "${e.httpStatus}|${e.errorCode}|${e.errorMessage}|${e.responseContent}")
            if (e.httpStatus == GitCodeApiStatus.FORBIDDEN.status) {
                error(
                    logMessage = "getProjectInfo error ${e.errorMessage}",
                    errorCode = ErrorCodeEnum.CREATE_NEW_FILE_ERROR_FORBIDDEN,
                    exceptionMessage = ErrorCodeEnum.CREATE_NEW_FILE_ERROR_FORBIDDEN.formatErrorMessage
                        .format(userId, gitCICreateFile.branch)
                )
            } else {
                error(
                    logMessage = "getProjectInfo error ${e.errorMessage}",
                    errorCode = ErrorCodeEnum.CREATE_NEW_FILE_ERROR,
                    exceptionMessage = ErrorCodeEnum.CREATE_NEW_FILE_ERROR.formatErrorMessage.format(e.errorMessage)
                )
            }
        } catch (e: Exception) {
            logger.error("getProjectInfo Exception: $e")
            error(" getProjectInfo error ${e.message}", ErrorCodeEnum.GET_PROJECT_INFO_ERROR)
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
        logger.info("getProjectMembers: [$gitProjectId|$token|$page|$pageSize|$search]")
        return client.getScm(ServiceGitCiResource::class).getMembers(
            token = token,
            gitProjectId = gitProjectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            search = search
        ).data
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
        logger.info("getProjectBranches: [$gitProjectId|$token|$page|$pageSize|$search|$orderBy|$sort]")
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
    fun getProjectId(isFork: Boolean = false, gitRequestEvent: GitRequestEvent): Long {
        with(gitRequestEvent) {
            return if (isFork) {
                sourceGitProjectId!!
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
        search: String?
    ): List<GitCodeProjectInfo>? {
        logger.info("getProjectList: [$accessToken|$userId|$page|$pageSize|$search]")
        return client.getScm(ServiceGitCiResource::class).getProjectList(
            accessToken = accessToken,
            userId = userId,
            page = page,
            pageSize = pageSize,
            search = search
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
    ): GitCIMrInfo? {
        logger.info("getMergeInfo: [$gitProjectId|$mergeRequestId][$token]")
        return retryFun(log = "$gitProjectId get mr $mergeRequestId info error",
            action = {
                client.getScm(ServiceGitResource::class).getGitCIMrInfo(
                    gitProjectId = gitProjectId,
                    mergeRequestId = mergeRequestId,
                    token = token
                ).data
            }
        )
    }

    fun getFileTreeFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        filePath: String,
        isMrEvent: Boolean = false
    ): List<GitFileInfo> {
        val gitProjectId = getProjectId(isMrEvent, gitRequestEvent)
        return try {
            val result = retryFun(log = "$gitProjectId get $filePath file tree error",
                action = {
                    client.getScm(ServiceGitResource::class).getGitCIFileTree(
                        gitProjectId = getProjectId(isMrEvent, gitRequestEvent),
                        path = filePath,
                        token = gitToken.accessToken,
                        ref = getTriggerBranch(gitRequestEvent.branch)
                    )
                }
            )
            result.data!!
        } catch (e: Throwable) {
            logger.error("$gitProjectId get $filePath file tree error", e)
            emptyList()
        }
    }

    private fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    private fun getOauthToken(userId: String, isEnableUser: Boolean, gitProjectId: Long): String {
        return if (isEnableUser) {
            val setting = gitCIBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
            oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
        } else {
            return oauthService.getAndCheckOauthToken(userId).accessToken
        }
    }

    @Throws(RuntimeException::class)
    fun <T> retryFun(log: String, action: () -> T): T {
        return try {
            return RetryUtils.clientRetry {
                action()
            }
        } catch (e: ClientException) {
            logger.error("retry 10 times $log: ${e.message} ")
            throw RuntimeException("$log | timeout ${e.message}")
        } catch (e: Exception) {
            logger.error("$log: ${e.message} ")
            throw RuntimeException("$log ${e.message}")
        }
    }

    // 返回给前端错误码异常
    private fun error(logMessage: String, errorCode: ErrorCodeEnum, exceptionMessage: String? = null) {
        logger.error(logMessage)
        throw ErrorCodeException(
            statusCode = 200,
            errorCode = errorCode.errorCode.toString(),
            defaultMessage = exceptionMessage ?: errorCode.formatErrorMessage
        )
    }
}
