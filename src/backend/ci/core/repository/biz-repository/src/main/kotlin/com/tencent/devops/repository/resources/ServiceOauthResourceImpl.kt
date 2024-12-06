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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitOauthCallback
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.tgit.TGitOAuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceOauthResourceImpl @Autowired constructor(
    private val gitOauthService: IGitOauthService,
    private val tGitOAuthService: TGitOAuthService,
    private val githubOAuthService: GithubOAuthService
) : ServiceOauthResource {
    override fun gitGet(userId: String): Result<GitToken?> {
        return Result(gitOauthService.getAccessToken(userId))
    }

    override fun tGitGet(userId: String): Result<GitToken?> {
        return Result(tGitOAuthService.getAccessToken(userId))
    }

    override fun gitCallback(code: String, state: String): Result<GitOauthCallback> {
        return Result(gitOauthService.gitCallback(code = code, state = state))
    }

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
                refreshToken = refreshToken
            )
        )
    }

    override fun tGitOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return Result(
            tGitOAuthService.isOAuth(
                userId = userId,
                redirectUrlType = redirectUrlType,
                redirectUrl = redirectUrl,
                gitProjectId = gitProjectId,
                refreshToken = refreshToken
            )
        )
    }

    override fun deleteOauth(userId: String, scmType: ScmType): Result<Boolean> {
        when (scmType) {
            ScmType.CODE_GIT -> {
                gitOauthService.deleteToken(userId)
            }

            ScmType.GITHUB -> {
                githubOAuthService.deleteToken(userId)
            }

            else -> {
                logger.warn("cannot delete oauth, not support scm type $scmType")
            }
        }
        return Result(true)
    }

    override fun reOauthUrl(userId: String, redirectUrl: String, scmType: ScmType): Result<String> {
        val url = when (scmType) {
            ScmType.CODE_GIT -> {
                gitOauthService.getOauthUrl(
                    userId = userId,
                    redirectUrl = redirectUrl
                )
            }

            ScmType.GITHUB -> {
                githubOAuthService.getGithubOauth(
                    userId = userId,
                    projectId = "",
                    redirectUrlTypeEnum = RedirectUrlTypeEnum.SPEC,
                    specRedirectUrl = redirectUrl,
                    repoHashId = null
                ).redirectUrl
            }

            else -> {
                logger.warn("cannot reset oauth, not support scm type $scmType")
                ""
            }
        }
        return Result(url)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceOauthResourceImpl::class.java)
    }
}
