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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
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
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletResponse

@Service
class TencentGitServiceImpl @Autowired constructor(val client: Client) : IGitService {

    override fun getProject(accessToken: String, userId: String): List<Project> {
        return client.get(ServiceGitResource::class).getProject(accessToken, userId).data ?: emptyList()
    }

    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        return client.get(ServiceGitResource::class).refreshToken(userId = userId, accessToken = accessToken).data!!
    }

    override fun getAuthUrl(authParamJsonStr: String): String {
        return client.get(ServiceGitResource::class).getAuthUrl(authParamJsonStr = authParamJsonStr).data!!
    }

    override fun getToken(userId: String, code: String): GitToken {
        return client.get(ServiceGitResource::class).getToken(userId = userId, code = code).data!!
    }

    override fun getRedirectUrl(redirectUrlType: String?): String {
        return client.get(ServiceGitResource::class).getRedirectUrl(authParamJsonStr = redirectUrlType!!).data!!
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): String {
        return client.get(ServiceGitResource::class).getGitFileContent(
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
        return client.get(ServiceGitResource::class).getGitlabFileContent(
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
        tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?> {
        return client.get(ServiceGitResource::class).createGitCodeRepository(
            userId = userId,
            token = token,
            repositoryName = repositoryName,
            sampleProjectPath = sampleProjectPath,
            namespaceId = namespaceId,
            visibilityLevel = visibilityLevel,
            tokenType = tokenType
        )
    }

    override fun addGitProjectMember(
        userIdList: List<String>,
        repoName: String,
        gitAccessLevel: GitAccessLevelEnum,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return client.get(ServiceGitResource::class).addGitProjectMember(
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
        return client.get(ServiceGitResource::class).deleteGitProjectMember(
            userIdList = userIdList,
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
        return client.get(ServiceGitResource::class).updateGitCodeRepository(
            token = token,
            projectName = projectName,
            updateGitProjectInfo = updateGitProjectInfo,
            tokenType = tokenType
        )
    }

    override fun moveProjectToGroup(
        groupCode: String,
        repoName: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        return client.get(ServiceGitResource::class).moveProjectToGroup(
            groupCode = groupCode,
            repositoryName = repoName,
            token = token,
            tokenType = tokenType
        )
    }

    override fun getMrInfo(repoName: String, mrId: Long, tokenType: TokenTypeEnum, token: String): GitMrInfo {
        return client.get(ServiceGitResource::class).getMergeRequestInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token
        ).data!!
    }

    override fun downloadGitRepoFile(
        repoName: String,
        sha: String?,
        token: String,
        tokenType: TokenTypeEnum,
        response: HttpServletResponse
    ) {
        client.get(ServiceGitResource::class).downloadGitRepoFile(
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
        token: String
    ): GitMrReviewInfo {
        return client.get(ServiceGitResource::class).getMergeRequestReviewersInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token
        ).data!!
    }

    override fun getMrChangeInfo(
        repoName: String,
        mrId: Long,
        tokenType: TokenTypeEnum,
        token: String
    ): GitMrChangeInfo {
        return client.get(ServiceGitResource::class).getMergeRequestChangeInfo(
            repoName = repoName,
            mrId = mrId,
            tokenType = tokenType,
            token = token
        ).data!!
    }
}