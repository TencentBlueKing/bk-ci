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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletResponse

@Primary
@Service
class TencentGitServiceImpl @Autowired constructor(val client: Client) : IGitService {

    override fun getProject(accessToken: String, userId: String): List<Project> {
        return client.getScm(ServiceGitResource::class).getProject(accessToken, userId).data ?: emptyList()
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
    ): List<Project> {
        return client.getScm(ServiceGitResource::class).getProjectList(
            accessToken = accessToken,
            userId = userId,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = orderBy,
            sort = sort,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data
            ?: emptyList()
    }

    override fun getBranch(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitBranch> {
        return client.getScm(ServiceGitResource::class).getBranch(
            accessToken = accessToken,
            userId = userId,
            repository = repository,
            page = page,
            pageSize = pageSize,
            search = search
        ).data ?: emptyList()
    }

    override fun getTag(
        accessToken: String,
        userId: String,
        repository: String,
        page: Int?,
        pageSize: Int?
    ): List<GitTag> {
        return client.getScm(ServiceGitResource::class).getTag(
            accessToken = accessToken,
            userId = userId,
            repository = repository,
            page = page,
            pageSize = pageSize
        ).data ?: emptyList()
    }

    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        return client.getScm(ServiceGitResource::class).refreshToken(userId = userId, accessToken = accessToken).data!!
    }

    override fun getAuthUrl(authParamJsonStr: String): String {
        return client.getScm(ServiceGitResource::class).getAuthUrl(authParamJsonStr = authParamJsonStr).data!!
    }

    override fun getToken(userId: String, code: String): GitToken {
        return client.getScm(ServiceGitResource::class).getToken(userId = userId, code = code).data!!
    }

    override fun getUserInfoByToken(token: String, tokenType: TokenTypeEnum): GitUserInfo {
        return client.getScm(ServiceGitResource::class)
            .getUserInfoByToken(token = token, useAccessToken = tokenType == TokenTypeEnum.OAUTH).data!!
    }

    override fun getRedirectUrl(authParamJsonStr: String): String {
        return client.getScm(ServiceGitResource::class).getRedirectUrl(authParamJsonStr = authParamJsonStr).data!!
    }

    override fun getGitFileContent(
        repoUrl: String?,
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        return client.getScm(ServiceGitResource::class).getGitFileContent(
            repoUrl = repoUrl,
            repoName = repoName,
            filePath = filePath,
            authType = authType,
            token = token,
            ref = ref
        ).data ?: ""
    }

    override fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): String {
        return client.getScm(ServiceGitResource::class).getGitlabFileContent(
            repoUrl = repoUrl,
            repoName = repoName,
            filePath = filePath,
            ref = ref,
            accessToken = accessToken
        ).data ?: ""
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
        return client.getScm(ServiceGitResource::class).createGitCodeRepository(
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
        repoName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).addGitProjectMember(
            userIdList = userIdList,
            repositorySpaceName = repoName,
            gitAccessLevel = gitAccessLevel,
            token = token,
            tokenType = tokenType
        )
    }

    override fun deleteGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).deleteGitProjectMember(
            userIdList = userIdList,
            repositorySpaceName = repoName,
            token = token,
            tokenType = tokenType
        )
    }

    override fun deleteGitProject(repoName: String, token: String, tokenType: TokenTypeEnum): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).deleteGitProject(
            repositorySpaceName = repoName,
            token = token,
            tokenType = tokenType
        )
    }

    override fun updateGitProjectInfo(
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).updateGitCodeRepository(
            token = token,
            projectName = projectName,
            updateGitProjectInfo = updateGitProjectInfo,
            tokenType = tokenType
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
        return client.getScm(ServiceGitResource::class).getGitRepositoryTreeInfo(
            userId = userId,
            repoName = repoName,
            refName = refName,
            path = path,
            token = token,
            tokenType = tokenType
        )
    }

    override fun moveProjectToGroup(
        groupCode: String,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        return client.getScm(ServiceGitResource::class).moveProjectToGroup(
            groupCode = groupCode,
            repositoryName = repoName,
            token = token,
            tokenType = tokenType
        )
    }

    override fun getMrInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrInfo {
        return client.getScm(ServiceGitResource::class).getMergeRequestInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token,
            repoUrl = repoUrl
        ).data!!
    }

    override fun downloadGitRepoFile(
        repoName: String,
        sha: String?,
        token: String,
        tokenType: TokenTypeEnum,
        response: HttpServletResponse
    ) {
        client.getScm(ServiceGitResource::class).downloadGitRepoFile(
            repoName = repoName,
            sha = sha,
            token = token,
            tokenType = tokenType,
            response = response
        )
    }

    override fun getMrReviewInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrReviewInfo {
        return client.getScm(ServiceGitResource::class).getMergeRequestReviewersInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token,
            repoUrl = repoUrl
        ).data!!
    }

    override fun getMrChangeInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String?
    ): GitMrChangeInfo {
        return client.getScm(ServiceGitResource::class).getMergeRequestChangeInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token,
            repoUrl = repoUrl
        ).data!!
    }

    override fun getRepoMembers(accessToken: String, userId: String, repoName: String): List<GitMember> {
        return client.getScm(ServiceGitResource::class).getRepoMembers(
            repoName = repoName,
            tokenType = TokenTypeEnum.OAUTH,
            token = accessToken
        ).data!!
    }

    override fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        return client.getScm(ServiceGitResource::class).getRepoRecentCommitInfo(
            repoName = repoName,
            sha = sha,
            token = token,
            tokenType = tokenType
        )
    }

    override fun getRepoMemberInfo(
        accessToken: String,
        userId: String,
        repoName: String,
        tokenType: TokenTypeEnum
    ): GitMember {
        return client.getScm(ServiceGitResource::class).getRepoMemberInfo(
            token = accessToken,
            userId = userId,
            gitProjectId = repoName,
            tokenType = tokenType
        ).data!!
    }

    override fun getRepoAllMembers(accessToken: String, userId: String, repoName: String): List<GitMember> {
        return client.getScm(ServiceGitResource::class).getRepoAllMembers(
            repoName = repoName,
            tokenType = TokenTypeEnum.OAUTH,
            token = accessToken
        ).data!!
    }

    override fun unlockHookLock(
        projectId: String?,
        repoName: String,
        mrId: Long
    ) {
        client.getScm(ServiceGitResource::class).unLockHookLock(
            projectId = projectId,
            repoName = repoName,
            mrId = mrId
        ).data!!
    }

    override fun getProjectGroupInfo(
        id: String,
        includeSubgroups: Boolean?,
        token: String,
        tokenType: TokenTypeEnum
    ): GitProjectGroupInfo {
        return client.getScm(ServiceGitResource::class).getProjectGroupInfo(
            id = id,
            includeSubgroups = includeSubgroups,
            token = token,
            tokenType = tokenType
        ).data!!
    }

    override fun createGitTag(
        repoName: String,
        tagName: String,
        ref: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).createGitTag(
            repoName = repoName,
            tagName = tagName,
            ref = ref,
            token = token,
            tokenType = tokenType
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
    ): List<ChangeFileInfo> {
        return client.getScm(ServiceGitResource::class).getChangeFileList(
            tokenType = tokenType,
            gitProjectId = gitProjectId,
            token = token,
            from = from,
            to = to,
            straight = straight,
            page = page,
            pageSize = pageSize
        ).data!!
    }

    override fun getGitProjectInfo(id: String, token: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        return client.getScm(ServiceGitResource::class).getGitProjectInfo(
            token = token,
            tokenType = tokenType,
            gitProjectId = id
        )
    }

    override fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?,
        tokenType: TokenTypeEnum
    ): List<GitCodeGroup> {
//        TODO("Not yet implemented")
        return client.getScm(ServiceGitResource::class).getProjectGroupsList(
            accessToken,
            page,
            pageSize,
            owned,
            minAccessLevel,
            tokenType
        ).data!!
    }

    override fun getMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>> {
        //        TODO("Not yet implemented")
        return client.getScm(ServiceGitResource::class).getMembers(
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
        return client.getScm(ServiceGitResource::class).getGitUserId(
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
        return client.getScm(ServiceGitResource::class).getProjectMembersAll(
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
        return client.getScm(ServiceGitResource::class).getGitFileInfo(
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
        return client.getScm(ServiceGitResource::class).addMrComment(
            token = token,
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody,
            tokenType = tokenType
        )
    }

    override fun getGitFileTree(
        gitProjectId: Long,
        path: String,
        token: String,
        ref: String?,
        recursive: Boolean?,
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>> {
        return client.getScm(ServiceGitResource::class).getGitCIFileTree(
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
        return client.getScm(ServiceGitResource::class).getCommits(
            gitProjectId,
            filePath,
            branch,
            token,
            since,
            until,
            page,
            perPage,
            tokenType
        )
    }

    override fun enableCi(
        projectName: String,
        token: String,
        tokenType: TokenTypeEnum,
        enable: Boolean?
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).enableCi(
            projectName = projectName,
            token = token,
            tokenType = tokenType,
            enable = enable
        )
    }

    override fun gitCreateFile(
        gitProjectId: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class).gitCreateFile(
            gitProjectId, token, gitOperationFile, tokenType
        )
    }

    override fun tGitUpdateFile(
        repoUrl: String?,
        repoName: String,
        token: String,
        gitOperationFile: GitOperationFile,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class)
            .tGitUpdateFile(
                repoUrl = repoUrl,
                repoName = repoName,
                token = token,
                gitOperationFile = gitOperationFile,
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
        return client.getScm(ServiceGitResource::class).getGitCodeProjectList(
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
        return client.getScm(ServiceGitResource::class).getTapdWorkItems(
            accessToken = accessToken,
            tokenType = tokenType,
            gitProjectId = gitProjectId,
            type = type,
            iid = iid
        )
    }

    override fun getTGitProjectInfo(
        id: String,
        token: String,
        tokenType: TokenTypeEnum,
        repoUrl: String
    ): Result<GitProjectInfo?> {
        return client.getScm(ServiceGitResource::class).getTGitProjectInfo(
            token = token,
            tokenType = tokenType,
            gitProjectId = id,
            repoUrl = repoUrl
        )
    }

    override fun getGitLabProjectInfo(
        id: String,
        token: String,
        tokenType: TokenTypeEnum,
        repoUrl: String
    ): Result<GitProjectInfo?> {
        return client.getScm(ServiceGitResource::class).getGitLabProjectInfo(
            token = token,
            tokenType = tokenType,
            gitProjectId = id,
            repoUrl = repoUrl
        )
    }
}
