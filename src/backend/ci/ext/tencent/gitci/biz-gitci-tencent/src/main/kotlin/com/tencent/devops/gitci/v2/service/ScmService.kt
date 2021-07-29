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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
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
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
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
        try {
            return client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!
        } catch (e: Exception) {
            throw RuntimeException("项目${gitProjectId}获取Token失败")
        }
    }

    fun getYamlFromGit(
        token: String,
        gitProjectId: Long,
        fileName: String,
        ref: String,
        useAccessToken: Boolean
    ): String {
        logger.info("getYamlFromGit: [$gitProjectId|$fileName|$token|$ref|$useAccessToken]")
        return try {
            val result = client.getScm(ServiceGitCiResource::class).getGitCIFileContent(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = token,
                ref = getTriggerBranch(ref),
                useAccessToken = useAccessToken
            )
            if (result.data == null) {
                throw RuntimeException("yaml $fileName is null")
            }
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            throw RuntimeException("Get yaml $fileName from git failed")
        }
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        logger.info("GitCIProjectInfo: [$gitProjectId|$token|$useAccessToken]")
        return try {
            val result = client.getScm(ServiceGitCiResource::class).getProjectInfo(
                accessToken = token,
                gitProjectId = gitProjectId,
                useAccessToken = useAccessToken
            )
            if (result.status.toString() == CommonMessageCode.SYSTEM_ERROR) {
                logger.error("getProjectInfo error [$gitProjectId|$token|$useAccessToken]")
                null
            } else {
                result.data
            }
        } catch (e: Exception) {
            logger.error("getProjectInfo error [$gitProjectId|$token|$useAccessToken]", e)
            null
        }
    }

    // 针对需要抛出异常做处理的场景
    fun getProjectInfoThrow(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        logger.info("getProjectInfoThrow: [$gitProjectId|$token|$useAccessToken]")
        val result = client.getScm(ServiceGitCiResource::class).getProjectInfo(
            accessToken = token,
            gitProjectId = gitProjectId,
            useAccessToken = useAccessToken
        )
        // 针对模板请求失败只有可能是无权限和系统问题，系统问题不考虑一律按无权限
        if (result.status.toString() == CommonMessageCode.SYSTEM_ERROR) {
            throw RuntimeException(PROJECT_PERMISSION_ERROR.format(gitProjectId))
        }
        return result.data
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
        token: String,
        gitProjectId: String,
        gitCICreateFile: GitCICreateFile
    ): Boolean {
        logger.info("createNewFile: [$gitProjectId|$token|$gitCICreateFile]")
        return client.getScm(ServiceGitResource::class).gitCICreateFile(
            gitProjectId = gitProjectId,
            token = token,
            gitCICreateFile = gitCICreateFile
        ).data!!
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
        return client.getScm(ServiceGitCiResource::class).getMergeRequestChangeInfo(
            token = if (userId == null) {
                token!!
            } else {
                getOauthToken(userId, true, gitProjectId)
            },
            gitProjectId = gitProjectId,
            mrId = mrId
        ).data
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
        logger.info("getProjectList: [$accessToken|$userId|$page|$pageSize|$search]")
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
        return client.getScm(ServiceGitCiResource::class).getGitFileInfo(
            gitProjectId = gitProjectId,
            filePath = filePath,
            ref = ref,
            token = token,
            useAccessToken = useAccessToken
        ).data
    }

    fun getMergeInfo(
        gitProjectId: Long,
        mergeRequestId: Long,
        token: String
    ): GitCIMrInfo? {
        logger.info("getMergeInfo: [$gitProjectId|$mergeRequestId][$token]")
        return client.getScm(ServiceGitResource::class).getGitCIMrInfo(
            gitProjectId = gitProjectId,
            mergeRequestId = mergeRequestId,
            token = token
        ).data
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
}
