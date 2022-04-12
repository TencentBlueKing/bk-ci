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
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitMrInfo
import com.tencent.devops.repository.pojo.git.GitMrReviewInfo
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.enums.GitProjectsOrderBy
import com.tencent.devops.scm.enums.GitSortAscOrDesc
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import javax.servlet.http.HttpServletResponse

@Suppress("ALL")
interface IGitService {
    fun getProject(accessToken: String, userId: String): List<Project>
    fun getBranch(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): List<GitBranch>
    fun getTag(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): List<GitTag>
    fun refreshToken(userId: String, accessToken: GitToken): GitToken
    fun getAuthUrl(authParamJsonStr: String): String
    fun getToken(userId: String, code: String): GitToken
    fun getUserInfoByToken(token: String): GitUserInfo
    fun getRedirectUrl(authParamJsonStr: String): String
    fun getGitFileContent(
        repoUrl: String? = null,
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String

    fun getProjectList(
        accessToken: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitProjectsOrderBy?,
        sort: GitSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<Project>

    fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): String

    fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum? = null
    ): Result<GitRepositoryResp?>

    fun getGitRepositoryTreeInfo(
        userId: String,
        repoName: String,
        refName: String?,
        path: String?,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?>

    fun addGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    fun deleteGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    fun deleteGitProject(
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    fun updateGitProjectInfo(
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    fun moveProjectToGroup(
        groupCode: String,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    fun getMrInfo(
        repoName: String,
        mrId: Long,
        tokenType:
        TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrInfo

    fun downloadGitRepoFile(
        repoName: String,
        sha: String?,
        token: String,
        tokenType: TokenTypeEnum,
        response: HttpServletResponse
    )

    fun getMrReviewInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrReviewInfo

    fun getMrChangeInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String,
        repoUrl: String? = null
    ): GitMrChangeInfo

    fun getRepoMembers(accessToken: String, userId: String, repoName: String): List<GitMember>

    fun getRepoMemberInfo(accessToken: String, userId: String, repoName: String, tokenType: TokenTypeEnum): GitMember

    fun getRepoAllMembers(accessToken: String, userId: String, repoName: String): List<GitMember>

    fun getRepoRecentCommitInfo(
        repoName: String,
        sha: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?>

    fun unlockHookLock(
        projectId: String? = "",
        repoName: String,
        mrId: Long
    )

    fun getProjectGroupInfo(
        id: String,
        includeSubgroups: Boolean?,
        token: String,
        tokenType: TokenTypeEnum
    ): GitProjectGroupInfo

    fun createGitTag(
        repoName: String,
        tagName: String,
        ref: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    fun getChangeFileList(
        token: String,
        tokenType: TokenTypeEnum,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean? = false,
        page: Int,
        pageSize: Int
    ): List<ChangeFileInfo>

    fun getGitProjectInfo(
        id: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    fun getProjectGroupList(
        accessToken: String,
        page: Int?,
        pageSize: Int?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?,
        tokenType: TokenTypeEnum
    ): List<GitCodeGroup>

    fun getMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>>

    fun getGitUserId(
        rtxUserId: String,
        gitProjectId: String,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<String?>

    fun getProjectMembersAll(
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        tokenType: TokenTypeEnum,
        token: String
    ): Result<List<GitMember>>

    fun getGitFileInfo(
        gitProjectId: String,
        filePath: String?,
        token: String,
        ref: String?,
        tokenType: TokenTypeEnum
    ): Result<GitCodeFileInfo>
}
