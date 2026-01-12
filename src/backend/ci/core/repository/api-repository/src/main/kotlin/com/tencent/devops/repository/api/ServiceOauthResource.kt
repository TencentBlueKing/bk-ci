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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitOauthCallback
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_REPOSITORY_OAUTH", description = "服务-oauth相关")
@Path("/service/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceOauthResource {

    @Operation(summary = "获取git代码库accessToken信息")
    @GET
    @Path("/git/{userId}")
    fun gitGet(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<GitToken?>

    @Operation(summary = "获取tgit代码库accessToken信息")
    @GET
    @Path("/tgit/{userId}")
    fun tGitGet(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<GitToken?>

    @Operation(summary = "工蜂回调请求")
    @GET
    @Path("/git/callback")
    fun gitCallback(
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String
    ): Result<GitOauthCallback>

    @Operation(summary = "根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @Parameter(description = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @Parameter(description = "工蜂项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long? = null,
        @Parameter(description = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = false
    ): Result<AuthorizeResult>

    @Operation(summary = "根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/tgit_oauth")
    fun tGitOAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @Parameter(description = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @Parameter(description = "工蜂项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long? = null,
        @Parameter(description = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = false
    ): Result<AuthorizeResult>

    @Operation(summary = "根据用户ID判断用户是否已经github oauth认证")
    @GET
    @Path("/github_oauth")
    fun githubOAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<AuthorizeResult>

    @Operation(summary = "根据用户ID判断用户是否已经github oauth认证")
    @GET
    @Path("/{scmCode}/isScmOauth")
    fun isScmOauth(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<Boolean>

    @Operation(summary = "获取accessToken信息[SCM_REPO]")
    @GET
    @Path("/{scmCode}/token/{oauthUserId}")
    fun scmRepoOauthToken(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "用户ID", required = true)
        @PathParam("oauthUserId")
        oauthUserId: String
    ): Result<GitToken?>

    @Operation(summary = "获取授权链接[SCM_REPO]")
    @GET
    @Path("/{scmCode}/oauthUrl")
    fun scmRepoOauthUrl(
        @Parameter(description = "蓝盾用户名", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "oauth认证成功后重定向到前端的地址", required = true)
        @QueryParam("redirectUrl")
        redirectUrl: String
    ): Result<String>
}
