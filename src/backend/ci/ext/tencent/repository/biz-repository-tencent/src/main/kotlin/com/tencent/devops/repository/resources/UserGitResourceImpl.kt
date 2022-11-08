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
import com.tencent.devops.repository.api.UserGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.pojo.Project
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitResourceImpl @Autowired constructor(
    private val gitOauthService: IGitOauthService
) : UserGitResource {

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return Result(
            gitOauthService.isOAuth(
                userId = userId,
                redirectUrlType = redirectUrlType,
                redirectUrl = redirectUrl,
                gitProjectId = gitProjectId,
                refreshToken
            )
        )
    }

    override fun deleteToken(userId: String): Result<Int> {
        return Result(gitOauthService.deleteToken(userId))
    }

    override fun getProject(
        userId: String,
        projectId: String,
        repoHashId: String?,
        search: String?
    ): Result<AuthorizeResult> {
        return Result(gitOauthService.getProject(userId, projectId, repoHashId, search))
    }

    override fun getProjectList(userId: String, page: Int?, pageSize: Int?): Result<List<Project>> {
        return Result(gitOauthService.getProjectList(userId, page, pageSize))
    }

    override fun getBranch(userId: String, repository: String, page: Int?, pageSize: Int?): Result<List<GitBranch>> {
        return Result(gitOauthService.getBranch(userId, repository, page, pageSize))
    }

    override fun getTag(userId: String, repository: String, page: Int?, pageSize: Int?): Result<List<GitTag>> {
        return Result(gitOauthService.getTag(userId, repository, page, pageSize))
    }
}
