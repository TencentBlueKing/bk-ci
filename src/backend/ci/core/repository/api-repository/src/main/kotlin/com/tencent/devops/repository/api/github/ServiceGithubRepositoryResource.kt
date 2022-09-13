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

package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.pojo.GithubRepo
import com.tencent.devops.common.sdk.github.pojo.GithubUser
import com.tencent.devops.common.sdk.github.pojo.RepositoryContent
import com.tencent.devops.common.sdk.github.pojo.RepositoryPermissions
import com.tencent.devops.common.sdk.github.request.CreateOrUpdateFileContentsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoryCollaboratorsRequest
import com.tencent.devops.common.sdk.github.request.SearchRepositoriesRequest
import com.tencent.devops.common.sdk.github.response.CreateOrUpdateFileContentsResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REPOSITORY_GITHUB"], description = "服务-github-repository")
@Path("/service/github/repository")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubRepositoryResource {
    @ApiOperation("创建或者更新文件内容")
    @POST
    @Path("/createOrUpdateFile")
    fun createOrUpdateFile(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CreateOrUpdateFileContentsRequest
    ): Result<CreateOrUpdateFileContentsResponse>

    @ApiOperation("获取仓库内容，可以是文件、文件夹")
    @POST
    @Path("/getRepositoryContent")
    fun getRepositoryContent(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetRepositoryContentRequest
    ): Result<RepositoryContent?>

    @ApiOperation("获取某个用户在某个仓库的权限和角色")
    @POST
    @Path("/getRepositoryPermissions")
    fun getRepositoryPermissions(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetRepositoryPermissionsRequest
    ): Result<RepositoryPermissions?>

    @ApiOperation("获取某个仓库的信息")
    @POST
    @Path("/getRepository")
    fun getRepository(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetRepositoryRequest
    ): Result<GithubRepo?>

    @ApiOperation("列出某个用户的仓库")
    @POST
    @Path("/listRepositories")
    fun listRepositories(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListRepositoriesRequest
    ): Result<List<GithubRepo>>

    @ApiOperation("列出某个仓库的所有成员")
    @POST
    @Path("/listRepositoryCollaborators")
    fun listRepositoryCollaborators(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListRepositoryCollaboratorsRequest
    ): Result<List<GithubUser>>

    @ApiOperation("查下有权限的仓库列表")
    @POST
    @Path("/searchRepositories")
    fun searchRepositories(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: SearchRepositoriesRequest
    ): Result<List<GithubRepo>>
}
