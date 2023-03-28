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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubAppUrl
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GITHUB_BK"], description = "服务-github相关")
@Path("/service/github")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubResource {

    @ApiOperation("创建github代码库accessToken")
    @POST
    @Path("/accessToken")
    fun createAccessToken(
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("accessToken类型", required = true)
        @QueryParam("tokenType")
        tokenType: String,
        @ApiParam("accessToken范围", required = true)
        @QueryParam("scope")
        scope: String
    ): Result<Boolean>

    @ApiOperation("获取github代码库accessToken")
    @GET
    @Path("/accessToken")
    fun getAccessToken(
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("token 类型", required = false)
        @QueryParam("tokenType")
        @DefaultValue("GITHUB_APP")
        tokenType: GithubTokenType? = GithubTokenType.GITHUB_APP
    ): Result<GithubToken?>

    @ApiOperation("获取github文件内容")
    @GET
    @Path("/getFileContent")
    fun getFileContent(
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("ref", required = true)
        @QueryParam("ref")
        ref: String,
        @ApiParam("filePath", required = true)
        @QueryParam("filePath")
        filePath: String
    ): Result<String>

    @ApiOperation("获取github触发原子配置")
    @GET
    @Path("/githubAppUrl")
    fun getGithubAppUrl(): Result<GithubAppUrl>

    @ApiOperation("创建github checkRuns")
    @POST
    @Path("/checkRuns")
    fun addCheckRuns(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        checkRuns: GithubCheckRuns
    ): Result<GithubCheckRunsResponse>

    @ApiOperation("更新github checkRuns")
    @PUT
    @Path("/checkRuns")
    fun updateCheckRuns(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("checkRunId", required = true)
        @QueryParam("checkRunId")
        checkRunId: Long,
        checkRuns: GithubCheckRuns
    ): Result<Boolean>

    @ApiOperation("获取github指定分支")
    @GET
    @Path("/getGithubBranch")
    fun getGithubBranch(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("branch", required = false)
        @QueryParam("branch")
        branch: String?
    ): Result<GithubBranch?>

    @ApiOperation("获取github指定tag")
    @GET
    @Path("/getGithubTag")
    fun getGithubTag(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("tag", required = true)
        @QueryParam("tag")
        tag: String
    ): Result<GithubTag?>

    @ApiOperation("List all the branches of github")
    @GET
    @Path("/branches")
    fun listBranches(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<List<String>>

    @ApiOperation("List all the branches of github")
    @GET
    @Path("/tags")
    fun listTags(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<List<String>>
}
