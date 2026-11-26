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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.repository.api.BuildOauthResource
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.RepositoryOauthService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.ScmTokenService
import com.tencent.devops.repository.service.tgit.TGitOAuthService
import com.tencent.devops.repository.utils.RepositoryUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOauthResourceImpl @Autowired constructor(
    private val gitOauthService: IGitOauthService,
    private val githubTokenService: GithubTokenService,
    private val tGitOAuthService: TGitOAuthService,
    private val scmTokenService: ScmTokenService,
    private val repositoryService: RepositoryService,
    private val repositoryOauthService: RepositoryOauthService
) : BuildOauthResource {

    @SensitiveApiPermission("get_oauth_token")
    override fun gitGet(projectId: String, buildId: String, userId: String): Result<GitToken?> {
        return Result(gitOauthService.checkAndGetAccessToken(projectId, buildId, userId))
    }

    @SensitiveApiPermission("get_oauth_token")
    override fun githubGet(projectId: String, buildId: String, userId: String): Result<GithubToken?> {
        return Result(githubTokenService.checkAndGetAccessToken(projectId, buildId, userId))
    }

    @SensitiveApiPermission("get_oauth_token")
    override fun tGitGet(projectId: String, buildId: String, userId: String): Result<GitToken?> {
        return Result(tGitOAuthService.checkAndGetAccessToken(projectId, buildId, userId))
    }

    override fun gitOauthUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ): Result<String> {
        return Result(
            repositoryOauthService.buildAuthorizeUrl(
                scmCode = ScmType.CODE_GIT.name,
                redirectUrl = getBuildUrl(projectId, pipelineId, buildId),
                oauthUserId = userId
            )
        )
    }

    override fun tGitOauthUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ): Result<String> {
        return Result(
            repositoryOauthService.buildAuthorizeUrl(
                scmCode = ScmType.CODE_TGIT.name,
                redirectUrl = getBuildUrl(projectId, pipelineId, buildId),
                oauthUserId = userId
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
            repositoryOauthService.buildAuthorizeUrl(
                scmCode = ScmType.GITHUB.name,
                redirectUrl = getBuildUrl(projectId, pipelineId, buildId),
                oauthUserId = userId
            )
        )
    }

    @SensitiveApiPermission("get_oauth_token")
    override fun scmRepoOauthToken(projectId: String, buildId: String, repoHashId: String): Result<GitToken?> {
        val repository = repositoryService.getRepository(
            projectId = projectId,
            repositoryHashId = repoHashId,
            null
        )
        val (isOauth, oauthUserId) = RepositoryUtils.getOauthUser(repository)
        if (!isOauth) {
            return Result(null)
        }

        return Result(
            scmTokenService.checkAndGetAccessToken(
                projectId = projectId,
                buildId = buildId,
                userId = oauthUserId,
                scmCode = repository.scmCode
            )
        )
    }

    override fun scmRepoOauthUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        scmCode: String,
        userId: String
    ): Result<String> {
        return Result(
            repositoryOauthService.buildAuthorizeUrl(
                scmCode = scmCode,
                redirectUrl = getBuildUrl(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                ),
                oauthUserId = userId
            )
        )
    }

    private fun getBuildUrl(projectId: String, pipelineId: String, buildId: String) =
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId/executeDetail"
}
