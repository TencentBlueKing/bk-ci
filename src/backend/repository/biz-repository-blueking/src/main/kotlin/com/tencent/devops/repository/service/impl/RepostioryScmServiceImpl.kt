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

package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.RepostioryScmService
import com.tencent.devops.repository.service.scm.GitService
import com.tencent.devops.repository.service.scm.SvnService
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.api.ServiceScmResource
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.TokenCheckResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepostioryScmServiceImpl @Autowired constructor(
    private val gitService: GitService,
    private val client: Client,
    private val svnService: SvnService
) : RepostioryScmService {
    override fun unlock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ): Boolean {
        return client.getScm(ServiceScmResource::class).unlock(
            projectName = projectName,
            url = url,
            type = type,
            region = region,
            userName = userName
        ).data ?: false
    }

    override fun lock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ): Boolean {
        return client.getScm(ServiceScmResource::class).lock(
            projectName,
            url,
            type,
            region,
            userName
        ).data ?: false
    }

    override fun moveProjectToGroup(
        token: String,
        groupCode: String,
        repositoryName: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        return client.getScm(ServiceGitResource::class)
            .moveProjectToGroup(token, groupCode, repositoryName, tokenType)
    }

    override fun updateGitCodeRepository(
        token: String,
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class)
            .updateGitCodeRepository(token, projectName, updateGitProjectInfo, tokenType)
    }

    override fun createGitCodeRepository(
        userId: String,
        token: String,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?> {
        return client.getScm(ServiceGitResource::class)
            .createGitCodeRepository(
                userId,
                token,
                repositoryName,
                sampleProjectPath,
                namespaceId,
                visibilityLevel,
                tokenType
            )
    }

    override fun addGitProjectMember(
        userIdList: List<String>,
        repositorySpaceName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class)
            .addGitProjectMember(userIdList, repositorySpaceName, gitAccessLevel, token, tokenType)
    }

    override fun deleteGitProjectMember(
        userIdList: List<String>,
        repositorySpaceName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.getScm(ServiceGitResource::class)
            .deleteGitProjectMember(userIdList, repositorySpaceName, token, tokenType)
    }

    override fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): Result<TokenCheckResult> {
        return client.getScm(ServiceScmResource::class)
            .checkPrivateKeyAndToken(projectName, url, type, privateKey, passPhrase, token, region, userName)
    }

    override fun checkUsernameAndPassword(
        projectName: String,
        url: String,
        type: ScmType,
        username: String,
        password: String,
        token: String,
        region: CodeSvnRegion?,
        repoUsername: String
    ): Result<TokenCheckResult> {
        return client.getScm(ServiceScmResource::class)
            .checkUsernameAndPassword(projectName, url, type, username, password, token, region, repoUsername)
    }

    override fun getProject(accessToken: String, userId: String): List<Project> {
        return gitService.getProject(accessToken, userId)
    }

    override fun getAuthUrl(authParamJsonStr: String): String {
        return gitService.getAuthUrl(authParamJsonStr)
    }

    override fun getToken(userId: String, code: String): GitToken {
        return gitService.getToken(userId, code)
    }

    override fun getRedirectUrl(authParamJsonStr: String): String {
        return gitService.getRedirectUrl(authParamJsonStr)
    }

    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        return gitService.refreshToken(userId, accessToken)
    }

    override fun getSvnFileContent(
        url: String,
        userId: String,
        svnType: String,
        filePath: String,
        reversion: Long,
        credential1: String,
        credential2: String?
    ): String {
        return svnService.getFileContent(url, userId, svnType, filePath, reversion, credential1, credential2)
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        return gitService.getGitFileContent(repoName, filePath, authType, token, ref)
    }

    override fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): String {
        return gitService.getGitlabFileContent(
            repoUrl = repoUrl,
            repoName = repoName,
            filePath = filePath,
            ref = ref,
            accessToken = accessToken
        )
    }
}