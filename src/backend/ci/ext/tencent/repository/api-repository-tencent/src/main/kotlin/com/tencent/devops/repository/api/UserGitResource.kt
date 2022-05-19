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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.pojo.Project
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_OAUTH_GIT"], description = "用户-git的oauth")
@Path("/user/git/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGitResource {

    @ApiOperation("根据用户ID, 通过oauth方式获取项目")
    @GET
    @Path("/getProject")
    fun getProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "repo hash iD", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?,
        @ApiParam(value = "工蜂代码库名字", required = false)
        @QueryParam("search")
        search: String? = null
    ): Result<AuthorizeResult>

    @ApiOperation("根据用户ID, 通过oauth方式获取项目")
    @GET
    @Path("/getProjectList")
    fun getProjectList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<Project>>

    @ApiOperation("根据用户ID, 通过oauth方式获取项目分支")
    @GET
    @Path("/getBranch")
    fun getBranch(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "仓库标识", required = true)
        @QueryParam("repository")
        repository: String,
        @ApiParam(value = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<GitBranch>>

    @ApiOperation("根据用户ID, 通过oauth方式获取项目Tag")
    @GET
    @Path("/getTag")
    fun getTag(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "仓库标识", required = true)
        @QueryParam("repository")
        repository: String,
        @ApiParam(value = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<GitTag>>

    @ApiOperation("删除用户的token ID")
    @DELETE
    @Path("/deleteToken")
    fun deleteToken(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Int>

    @ApiOperation("根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @ApiParam(value = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @ApiParam(value = "工蜂项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long? = null,
        @ApiParam(value = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = false
    ): Result<AuthorizeResult>
}
