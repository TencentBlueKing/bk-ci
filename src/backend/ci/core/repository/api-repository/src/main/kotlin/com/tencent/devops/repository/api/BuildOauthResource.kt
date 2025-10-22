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

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_REPOSITORY_OAUTH", description = "构建-oauth相关")
@Path("/build/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildOauthResource {

    @Operation(summary = "获取git代码库accessToken信息")
    @GET
    @Path("/git/{userId}")
    fun gitGet(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<GitToken?>

    @Operation(summary = "获取git代码库accessToken信息")
    @GET
    @Path("/github/{userId}")
    fun githubGet(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<GithubToken?>

    @Operation(summary = "获取授权链接")
    @GET
    @Path("/git/oauthUrl")
    fun gitOauthUrl(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @QueryParam("userId")
        userId: String
    ): Result<String>

    @Operation(summary = "获取授权链接")
    @GET
    @Path("/github/oauthUrl")
    fun githubOauthUrl(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @QueryParam("userId")
        userId: String
    ): Result<String>

    @Operation(summary = "获取accessToken信息[SCM_REPO]")
    @GET
    @Path("scm/repo/{repoHashId}")
    fun scmRepoOauthToken(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "代码库HashId", required = true)
        @PathParam("repoHashId")
        repoHashId: String
    ): Result<GitToken?>

    @Operation(summary = "获取授权链接[SCM_REPO]")
    @GET
    @Path("/{scmCode}/oauthUrl")
    fun scmRepoOauthUrl(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<String>
}
