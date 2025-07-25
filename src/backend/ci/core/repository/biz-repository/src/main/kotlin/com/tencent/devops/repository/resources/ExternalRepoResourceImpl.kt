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

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalRepoResource
import com.tencent.devops.repository.sdk.tapd.service.ITapdOauthService
import com.tencent.devops.repository.service.RepositoryOauthService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.tgit.TGitOAuthService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

@RestResource
class ExternalRepoResourceImpl @Autowired constructor(
    private val gitOauthService: IGitOauthService,
    private val tapdService: ITapdOauthService,
    private val tGitOAuthService: TGitOAuthService,
    private val repositoryOauthService: RepositoryOauthService
) : ExternalRepoResource {
    override fun gitCallback(code: String, state: String): Response {
        val gitOauthCallback = gitOauthService.gitCallback(code, state)
        return Response.temporaryRedirect(UriBuilder.fromUri(gitOauthCallback.redirectUrl).build()).build()
    }

    override fun tGitCallback(code: String, state: String): Response {
        val gitOauthCallback = tGitOAuthService.gitCallback(code, state)
        return Response.temporaryRedirect(UriBuilder.fromUri(gitOauthCallback.redirectUrl).build()).build()
    }

    override fun tapdCallback(code: String, state: String, resource: String): Response {
        val uri = UriBuilder.fromUri(
            tapdService.callbackUrl(
                code = code,
                state = state,
                resource = resource
            )
        ).build()
        return Response.temporaryRedirect(uri).build()
    }

    override fun scmCallback(scmCode: String, code: String, state: String): Response {
        val redirectUrl = repositoryOauthService.oauthCallback(scmCode = scmCode, code = code, state = state)
        return Response.temporaryRedirect(UriBuilder.fromUri(redirectUrl).build()).build()
    }
}
