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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.resources.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitMrInfo
import com.tencent.devops.repository.pojo.git.GitMrReviewInfo
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.pojo.GitRepositoryResp
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletResponse

@RestResource
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

    override fun getProjectList(accessToken: String, userId: String, page: Int?, pageSize: Int?): Result<List<Project>> {
        return Result(gitService.getProjectList(accessToken, userId, page, pageSize))
    }

    override fun getBranch(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): Result<List<GitBranch>> {
        return Result(gitService.getBranch(userId, accessToken, repository, page, pageSize))
    }

    override fun getTag(accessToken: String, userId: String, repository: String, page: Int?, pageSize: Int?): Result<List<GitTag>> {
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
        return Result(gitService.getGitFileContent(repoName, filePath, authType, token, ref))
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
        tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?> {
        return gitService.createGitCodeRepository(userId, token, repositoryName, sampleProjectPath, namespaceId, visibilityLevel, tokenType)
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
}