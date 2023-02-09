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

package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.enums.GitProjectsOrderBy
import com.tencent.devops.scm.enums.GitSortAscOrDesc
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCICommitRef
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIFileCommit
import com.tencent.devops.scm.pojo.GitCIMrInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.scm.services.GitService
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletResponse

@RestResource
class ServiceGitResourceImpl @Autowired constructor(
    private val gitService: GitService
) : ServiceGitResource {

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

    override fun deleteGitProject(
        repositorySpaceName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.deleteGitProject(repositorySpaceName, token, tokenType)
    }

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

    override fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String?,
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

    override fun getGitRepositoryTreeInfo(
        userId: String,
        repoName: String,
        refName: String?,
        path: String?,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?> {
        return gitService.getGitRepositoryTreeInfo(
            userId = userId,
            repoName = repoName,
            refName = refName,
            path = path,
            token = token,
            tokenType = tokenType
        )
    }

    override fun getProject(accessToken: String, userId: String): Result<List<Project>> {
        return Result(gitService.getProject(accessToken, userId))
    }

    override fun getProjectInfo(
        accessToken: String,
        gitProjectId: Long
    ): Result<GitCIProjectInfo?> {
        return gitService.getGitCIProjectInfo(gitProjectId.toString(), accessToken)
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
                accessToken = accessToken,
                userId = userId,
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

    override fun getToken(gitProjectId: Long): Result<GitToken> {
        return Result(gitService.getToken(gitProjectId.toString()))
    }

    override fun getUserInfoByToken(
        token: String,
        useAccessToken: Boolean
    ): Result<GitUserInfo> {
        return Result(
            gitService.getUserInfoByToken(
                token = token,
                useAccessToken = useAccessToken
            )
        )
    }

    override fun getGitCIFileContent(
        gitProjectId: Long,
        filePath: String,
        token: String,
        ref: String
    ): Result<String> {
        return Result(gitService.getGitCIFileContent(gitProjectId, filePath, token, ref))
    }

    override fun getGitCIMrChanges(gitProjectId: Long, mergeRequestId: Long, token: String): Result<GitMrChangeInfo> {
        return Result(gitService.getGitCIMrChanges(gitProjectId, mergeRequestId, token))
    }

    override fun getGitCIMrInfo(gitProjectId: Long, mergeRequestId: Long, token: String): Result<GitCIMrInfo> {
        return Result(gitService.getGitCIMrInfo(gitProjectId, mergeRequestId, token))
    }

    override fun getFileCommits(
        gitProjectId: Long,
        filePath: String,
        branch: String,
        token: String
    ): Result<List<GitCIFileCommit>> {
        return Result(gitService.getFileCommits(gitProjectId, filePath, branch, token))
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
        return Result(
            gitService.getCommits(
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
        )
    }

    override fun gitCICreateFile(
        gitProjectId: String,
        token: String,
        gitCreateFile: GitCICreateFile
    ): Result<Boolean> {
        return Result(
            gitService.gitCodeCreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitCreateFile = gitCreateFile,
                tokenType = TokenTypeEnum.OAUTH
            )
        )
    }

    override fun getCommitRefs(
        gitProjectId: Long,
        commitId: String,
        type: String,
        token: String
    ): Result<List<GitCICommitRef>> {
        return Result(gitService.getCommitRefs(gitProjectId, commitId, type, token))
    }

    override fun getGitCIFileTree(
        gitProjectId: Long,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>> {
        return Result(
            gitService.getGitCIFileTree(
                gitProjectId = gitProjectId,
                path = path,
                token = token,
                ref = ref,
                recursive = recursive,
                tokenType = tokenType
            )
        )
    }

    override fun getRedirectUrl(authParamJsonStr: String): Result<String> {
        return Result(gitService.getRedirectUrl(authParamJsonStr))
    }

    override fun getGitFileContent(
        repoUrl: String?,
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): Result<String> {
        return Result(
            gitService.getGitFileContent(
                repoUrl = repoUrl,
                repoName = repoName,
                filePath = filePath,
                authType = authType,
                token = token,
                ref = ref
            )
        )
    }

    override fun getGitlabFileContent(
        repoUrl: String?,
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

    override fun getMergeRequestInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): Result<GitMrInfo> {
        return Result(
            gitService.getMrInfo(
                id = repoName,
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
                id = repoName,
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
                id = repoName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repoUrl
            )
        )
    }

    override fun getRepoMembers(repoName: String, tokenType: TokenTypeEnum, token: String): Result<List<GitMember>> {
        return Result(gitService.getRepoMembers(repoName, tokenType, token))
    }

    override fun getRepoMemberInfo(
        token: String,
        userId: String,
        gitProjectId: String,
        tokenType: TokenTypeEnum
    ): Result<GitMember> {
        return Result(gitService.getRepoMemberInfo(token, userId, gitProjectId, tokenType))
    }

    override fun getRepoAllMembers(repoName: String, tokenType: TokenTypeEnum, token: String): Result<List<GitMember>> {
        return Result(gitService.getRepoAllMembers(repoName, tokenType, token))
    }

    override fun addCommitCheck(request: CommitCheckRequest): Result<Boolean> {
        gitService.addCommitCheck(request)
        return Result(true)
    }

    override fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        return gitService.getRepoRecentCommitInfo(repoName, sha, token, tokenType)
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

    override fun createGitTag(
        repoName: String,
        tagName: String,
        ref: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.createGitTag(
            repoName = repoName,
            tagName = tagName,
            ref = ref,
            token = token,
            tokenType = tokenType
        )
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

    override fun getGitProjectInfo(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String
    ): Result<GitProjectInfo?> {
        return gitService.getGitProjectInfo(
            id = gitProjectId,
            token = token,
            tokenType
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
            gitService.getProjectGroupsList(
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
            token = token,
            tokenType = tokenType
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
        return gitService.addMrComment(
            token = token,
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody,
            tokenType = tokenType
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

    override fun enableCi(
        projectName: String,
        token: String,
        tokenType: TokenTypeEnum,
        enable: Boolean?
    ): Result<Boolean> {
        return gitService.enableCi(projectName = projectName, token = token, tokenType = tokenType, enable = enable)
    }

    override fun gitCreateFile(
        gitProjectId: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return Result(
            gitService.gitCreateFile(
                gitProjectId = gitProjectId,
                token = token,
                gitOperationFile = gitOperationFile,
                tokenType = tokenType
            )
        )
    }

    override fun tGitUpdateFile(
        repoUrl: String?,
        repoName: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return Result(
            gitService.tGitUpdateFile(
                repoUrl = repoUrl,
                repoName = repoName,
                token = token,
                gitOperationFile = gitOperationFile,
                tokenType = tokenType
            )
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
}
