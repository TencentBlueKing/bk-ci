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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubAppUrl
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.github.IGithubService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubResourceImpl @Autowired constructor(
    private val githubTokenService: GithubTokenService,
    private val githubService: IGithubService,
    private val githubOAuthService: GithubOAuthService
) : ServiceGithubResource {
    override fun createAccessToken(
        userId: String,
        accessToken: String,
        tokenType: String,
        scope: String
    ): Result<Boolean> {
        githubTokenService.createAccessToken(userId, accessToken, tokenType, scope)
        return Result(true)
    }

    override fun getAccessToken(userId: String, tokenType: GithubTokenType?): Result<GithubToken?> {
        return Result(githubTokenService.getAccessToken(userId, tokenType ?: GithubTokenType.GITHUB_APP))
    }

    override fun getFileContent(projectName: String, ref: String, filePath: String): Result<String> {
        return Result(githubService.getFileContent(projectName, ref, filePath))
    }

    override fun getGithubAppUrl(): Result<GithubAppUrl> {
        return Result(githubOAuthService.getGithubAppUrl())
    }

    override fun addCheckRuns(
        accessToken: String,
        projectName: String,
        checkRuns: GithubCheckRuns
    ): Result<GithubCheckRunsResponse> {
        return Result(githubService.addCheckRuns(accessToken, projectName, checkRuns))
    }

    override fun updateCheckRuns(
        accessToken: String,
        projectName: String,
        checkRunId: Long,
        checkRuns: GithubCheckRuns
    ): Result<Boolean> {
        githubService.updateCheckRuns(accessToken, projectName, checkRunId, checkRuns)
        return Result(true)
    }

    override fun getGithubBranch(accessToken: String, projectName: String, branch: String?): Result<GithubBranch?> {
        return Result(githubService.getBranch(accessToken, projectName, branch))
    }

    override fun getGithubTag(accessToken: String, projectName: String, tag: String): Result<GithubTag?> {
        return Result(githubService.getTag(accessToken, projectName, tag))
    }

    override fun listBranches(accessToken: String, projectName: String): Result<List<String>> {
        return Result(githubService.listBranches(accessToken, projectName))
    }

    override fun listTags(accessToken: String, projectName: String): Result<List<String>> {
        return Result(githubService.listTags(accessToken, projectName))
    }
}
