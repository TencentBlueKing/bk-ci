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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.repository.api.BuildOauthResource
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.scm.IGitOauthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOauthResourceImpl @Autowired constructor(
    private val gitOauthService: IGitOauthService,
    private val githubTokenService: GithubTokenService,
    private val githubOAuthService: GithubOAuthService
) : BuildOauthResource {

    @SensitiveApiPermission("get_oauth_token")
    override fun gitGet(projectId: String, buildId: String, userId: String): Result<GitToken?> {
        return Result(gitOauthService.checkAndGetAccessToken(projectId, buildId, userId))
    }

    override fun githubGet(projectId: String, buildId: String, userId: String): Result<GithubToken?> {
        return Result(githubTokenService.checkAndGetAccessToken(projectId, buildId, userId))
    }

    override fun gitOauthUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ): Result<String> {
        return Result(
            gitOauthService.getOauthUrl(
                userId = userId,
                redirectUrl = getBuildUrl(projectId, pipelineId, buildId)
            )
        )
    }

    override fun githubOauthUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ): Result<String> {
        return Result(
            githubOAuthService.getGithubOauth(
                userId = userId,
                projectId = projectId,
                redirectUrlTypeEnum = RedirectUrlTypeEnum.SPEC,
                specRedirectUrl = getBuildUrl(projectId, pipelineId, buildId),
                repoHashId = null
            ).redirectUrl
        )
    }

    private fun getBuildUrl(projectId: String, pipelineId: String, buildId: String) =
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId/executeDetail"
}
