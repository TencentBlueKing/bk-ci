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
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.pojo.Project
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_OAUTH_GIT", description = "用户-git的oauth")
@Path("/user/tgit/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTGitResource {

    @Operation(summary = "根据用户ID, 通过oauth方式获取项目")
    @GET
    @Path("/getProject")
    fun getProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash iD", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?,
        @Parameter(description = "工蜂代码库名字", required = false)
        @QueryParam("search")
        search: String? = null
    ): Result<AuthorizeResult>

    @Operation(summary = "根据用户ID, 通过oauth方式获取项目")
    @GET
    @Path("/getProjectList")
    fun getProjectList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<Project>>

    @Operation(summary = "根据用户ID, 通过oauth方式获取项目分支")
    @GET
    @Path("/getBranch")
    fun getBranch(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库标识", required = true)
        @QueryParam("repository")
        repository: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<GitBranch>>

    @Operation(summary = "根据用户ID, 通过oauth方式获取项目Tag")
    @GET
    @Path("/getTag")
    fun getTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库标识", required = true)
        @QueryParam("repository")
        repository: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<GitTag>>

    @Operation(summary = "删除用户的token ID")
    @DELETE
    @Path("/deleteToken")
    fun deleteToken(
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Int>

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
        refreshToken: Boolean? = false,
        @Parameter(description = "是否校验token(refreshToken=true时不做校验)", required = false)
        @QueryParam("validationCheck")
        validationCheck: Boolean? = false
    ): Result<AuthorizeResult>
}
