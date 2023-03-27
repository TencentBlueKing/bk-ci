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

package com.tencent.devops.repository.resources.scm

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.enums.GitProjectsOrderBy
import com.tencent.devops.scm.enums.GitSortAscOrDesc
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.pojo.TapdWorkItem
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletResponse

@RestResource
@Suppress("ALL")
class ServiceGitResourceImpl @Autowired constructor(
    private val gitService: IGitService
) : ServiceGitResource {

    override fun moveProjectToGroup(
        token: String,
        groupCode: String,
        repositoryName: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        return gitService.moveProjectToGroup(groupCode, repositoryName, token, tokenType)
    }

    override fun updateGitCodeRepository(
        token: String,
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.updateGitProjectInfo(projectName, updateGitProjectInfo, token, tokenType)
    }

    override fun getProject(accessToken: String, userId: String): Result<List<Project>> {
        return Result(gitService.getProject(accessToken, userId))
    }

    override fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitProjectsOrderBy?,
        sort: GitSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<Project>> {
        return Result(
            gitService.getProjectList(
                accessToken = accessToken,
                userId = userId,
                page = page,
                pageSize = pageSize,
                search = search,
                orderBy = orderBy,
                sort = sort,
                owned = owned,
                minAccessLevel = minAccessLevel
            )
        )
    }

    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): Result<List<GitBranch>> {
        return Result(
            gitService.getBranch(
                userId = userId,
                accessToken = accessToken,
                repository = repository,
                page = page,
                pageSize = pageSize,
                search = search
            )
        )
    }

    override fun getTag(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<GitTag>> {
        return Result(gitService.getTag(accessToken, userId, repository, page, pageSize))
    }

    override fun refreshToken(userId: String, accessToken: GitToken): Result<GitToken> {
        return Result(gitService.refreshToken(userId, accessToken))
    }

    override fun getAuthUrl(authParamJsonStr: String): Result<String> {
        return Result(gitService.getAuthUrl(authParamJsonStr))
    }

    override fun getToken(userId: String, code: String): Result<GitToken> {
        return Result(gitService.getToken(userId, code))
    }

    override fun getRedirectUrl(authParamJsonStr: String): Result<String> {
        return Result(gitService.getRedirectUrl(authParamJsonStr))
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): Result<String> {
        return Result(
            gitService.getGitFileContent(
                repoUrl = null,
                repoName = repoName,
                filePath = filePath,
                authType = authType,
                token = token,
                ref = ref
            )
        )
    }

    override fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): Result<String> {
        return Result(
            gitService.getGitlabFileContent(
                repoUrl = repoUrl,
                repoName = repoName,
                filePath = filePath,
                ref = ref,
                accessToken = accessToken
            )
        )
    }

    override fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum?
    ): Result<GitRepositoryResp?> {
        return gitService.createGitCodeRepository(
            userId = userId,
            token = token,
            repositoryName = repositoryName,
            sampleProjectPath = sampleProjectPath,
            namespaceId = namespaceId,
            visibilityLevel = visibilityLevel,
            tokenType = tokenType,
            frontendType = frontendType
        )
    }

    override fun addGitProjectMember(
        userIdList: List<String>,
        repositorySpaceName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.addGitProjectMember(userIdList, repositorySpaceName, gitAccessLevel, token, tokenType)
    }

    override fun deleteGitProjectMember(
        userIdList: List<String>,
        repositorySpaceName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.deleteGitProjectMember(userIdList, repositorySpaceName, token, tokenType)
    }

    override fun getMergeRequestInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): Result<GitMrInfo> {
        return Result(
            gitService.getMrInfo(
                repoName = repoName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repoUrl
            )
        )
    }

    override fun downloadGitRepoFile(
        repoName: String,
        sha: String?,
        token: String,
        tokenType: TokenTypeEnum,
        response: HttpServletResponse
    ) {
        return gitService.downloadGitRepoFile(repoName, sha, token, tokenType, response)
    }

    override fun getMergeRequestReviewersInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): Result<GitMrReviewInfo> {
        return Result(
            gitService.getMrReviewInfo(
                repoName = repoName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repoUrl
            )
        )
    }

    override fun getMergeRequestChangeInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): Result<GitMrChangeInfo> {
        return Result(
            gitService.getMrChangeInfo(
                repoName = repoName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repoUrl
            )
        )
    }

    override fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        return gitService.getRepoRecentCommitInfo(repoName = repoName, sha = sha, token = token, tokenType = tokenType)
    }

    override fun unLockHookLock(
        projectId: String?,
        repoName: String,
        mrId: Long
    ): Result<Boolean> {
        gitService.unlockHookLock(
            projectId = projectId,
            repoName = repoName,
            mrId = mrId
        )
        return Result(true)
    }

    override fun getProjectGroupInfo(
        id: String,
        includeSubgroups: Boolean?,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectGroupInfo> {
        return Result(
            gitService.getProjectGroupInfo(
                id = id,
                includeSubgroups = includeSubgroups,
                token = token,
                tokenType = tokenType
            )
        )
    }

    override fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<List<ChangeFileInfo>> {
        return Result(
            gitService.getChangeFileList(
                tokenType = tokenType,
                gitProjectId = gitProjectId,
                token = token,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getProjectInfo(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String
    ): Result<GitProjectInfo?> {
        return gitService.getGitProjectInfo(
            id = gitProjectId,
            token = token,
            tokenType = tokenType
        )
    }

    override fun getProjectUserInfo(
        token: String,
        userId: String,
        gitProjectId: String,
        tokenType: TokenTypeEnum
    ): Result<GitMember> {
        return Result(
            gitService.getRepoMemberInfo(
                accessToken = token,
                userId = userId,
                repoName = gitProjectId,
                tokenType = tokenType
            )
        )
    }

    override fun getProjectGroupsList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?,
        tokenType: TokenTypeEnum
    ): Result<List<GitCodeGroup>> {
        return Result(
            gitService.getProjectGroupList(
                accessToken = accessToken,
                page = page,
                pageSize = pageSize,
                owned = owned,
                minAccessLevel = minAccessLevel,
                tokenType = tokenType
            )
        )
    }

    override fun getMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>> {
        return gitService.getMembers(
            token = token,
            gitProjectId = gitProjectId,
            page = page,
            pageSize = pageSize,
            search = search,
            tokenType = tokenType
        )
    }

    override fun getGitUserId(
        rtxUserId: String,
        gitProjectId: String,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<String?> {
        return gitService.getGitUserId(
            rtxUserId = rtxUserId,
            gitProjectId = gitProjectId,
            tokenType = tokenType,
            token = token
        )
    }

    override fun getProjectMembersAll(
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<List<GitMember>> {
        return gitService.getProjectMembersAll(
            gitProjectId = gitProjectId,
            page = page,
            pageSize = pageSize,
            search = search,
            tokenType = tokenType,
            token = token
        )
    }

    override fun getGitFileInfo(
        gitProjectId: String,
        filePath: String?,
        token: String,
        ref: String?,
        tokenType: TokenTypeEnum
    ): Result<GitCodeFileInfo> {
        return gitService.getGitFileInfo(
            gitProjectId = gitProjectId,
            filePath = filePath,
            token = token,
            ref = ref,
            tokenType = tokenType
        )
    }

    override fun addMrComment(
        token: String,
        gitProjectId: String,
        mrId: Long,
        mrBody: String,
        tokenType: TokenTypeEnum
    ) {
        gitService.addMrComment(
            token = token,
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody,
            tokenType = tokenType
        )
    }

    override fun getGitFileTree(
        gitProjectId: String,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>> {
        return gitService.getGitFileTree(
            gitProjectId = gitProjectId,
            path = path,
            token = token,
            ref = ref,
            recursive = recursive,
            tokenType = tokenType
        )
    }

    override fun getCommits(
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        token: String,
        since: String?,
        until: String?,
        page: Int,
        perPage: Int,
        tokenType: TokenTypeEnum
    ): Result<List<Commit>> {
        return gitService.getCommits(
            gitProjectId = gitProjectId,
            filePath = filePath,
            branch = branch,
            token = token,
            since = since,
            until = until,
            page = page,
            perPage = perPage,
            tokenType = tokenType
        )
    }

    override fun enableCi(
        projectName: String,
        token: String,
        tokenType: TokenTypeEnum,
        enable: Boolean?
    ): Result<Boolean> {
        return gitService.enableCi(
            projectName = projectName, token = token, tokenType = tokenType, enable = enable
        )
    }

    override fun gitCreateFile(
        gitProjectId: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.gitCreateFile(
            gitProjectId = gitProjectId,
            token = token,
            gitOperationFile = gitOperationFile,
            tokenType = tokenType
        )
    }

    override fun getUserInfoByToken(
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitUserInfo> {
        return Result(
            gitService.getUserInfoByToken(
                token,
                tokenType
            )
        )
    }

    override fun getUserInfoById(userId: String, token: String, tokenType: TokenTypeEnum): Result<GitUserInfo> {
        return Result(
            gitService.getUserInfoById(
                userId,
                token,
                tokenType
            )
        )
    }

    override fun getGitCodeProjectList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>> {
        return gitService.getGitCodeProjectList(
            accessToken = accessToken,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = orderBy,
            sort = sort,
            owned = owned,
            minAccessLevel = minAccessLevel
        )
    }

    override fun getTapdWorkItems(
        accessToken: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        type: String,
        iid: Long
    ): Result<List<TapdWorkItem>> {
        return gitService.getTapdWorkItems(
            accessToken = accessToken,
            tokenType = tokenType,
            gitProjectId = gitProjectId,
            type = type,
            iid = iid
        )
    }

    override fun getCommitDiff(
        accessToken: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        sha: String,
        path: String?,
        ignoreWhiteSpace: Boolean?
    ): Result<List<GitDiff>> {
        return gitService.getCommitDiff(
            accessToken = accessToken,
            tokenType = tokenType,
            gitProjectId = gitProjectId,
            sha = sha,
            path = path,
            ignoreWhiteSpace = ignoreWhiteSpace
        )
    }
}
