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

package com.tencent.devops.gitci.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_GIT_CI_GIT_CODE"], description = "user-工蜂接口访问")
@Path("/user/gitcode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGitCIGitCodeResource {

    @ApiOperation("获取工蜂项目信息")
    @GET
    @Path("/info")
    fun getGitCodeProjectInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitCIProjectInfo?>

    @ApiOperation("获取工蜂项目下所有触发人信息")
    @GET
    @Path("/members")
    fun getGitCodeProjectMembers(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = true)
        @QueryParam("search")
        search: String?
    ): Result<List<GitMember>?>

    @ApiOperation("获取工蜂项目所有提交信息")
    @GET
    @Path("/commits")
    fun getGitCodeCommits(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @ApiParam(value = "分支名称")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @ApiParam(value = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @ApiParam(value = "页码", defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数量,最大100", defaultValue = "20")
        @QueryParam("perPage")
        perPage: Int
    ): Result<List<Commit>?>

    @ApiOperation("向工蜂项目中创建新文件")
    @POST
    @Path("/repository/files")
    fun gitCodeCreateFile(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "创建文件内容")
        gitCICreateFile: GitCICreateFile
    ): Result<Boolean>
}
