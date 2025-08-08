/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.service.github

import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.sdk.github.pojo.RepositoryPermissions
import com.tencent.devops.repository.sdk.github.response.GetUserResponse

interface IGithubService {

    fun webhookCommit(event: String, guid: String, signature: String, body: String)

    fun addCheckRuns(
        token: String,
        projectName: String,
        checkRuns: GithubCheckRuns
    ): GithubCheckRunsResponse

    fun updateCheckRuns(
        token: String,
        projectName: String,
        checkRunId: Long,
        checkRuns: GithubCheckRuns
    )

    fun getProject(
        projectId: String,
        userId: String,
        repoHashId: String?,
        oauthUserId: String?
    ): AuthorizeResult

    fun getBranch(token: String, projectName: String, branch: String?): GithubBranch?

    fun getTag(token: String, projectName: String, tag: String): GithubTag?

    fun getFileContent(projectName: String, ref: String, filePath: String, token: String = ""): String

    fun listBranches(token: String, projectName: String): List<String>

    fun listTags(token: String, projectName: String): List<String>

    fun isOAuth(
        userId: String,
        projectId: String,
        refreshToken: Boolean?,
        resetType: String?,
        redirectUrlType: RedirectUrlTypeEnum? = null,
        redirectUrl: String? = ""
    ): AuthorizeResult

    fun getAccessToken(userId: String): GithubToken?

    fun getUser(token: String): GetUserResponse?

    fun getRepositoryPermissions(projectName: String, userId: String, token: String): RepositoryPermissions?
}
