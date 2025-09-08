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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STREAM_GIT_CODE", description = "user-stream 接口访问")
@Path("/user/gitcode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamGitResource {

    @Operation(summary = "获取stream 项目信息")
    @GET
    @Path("/projects/info")
    fun getGitCodeProjectInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "stream 项目路径/id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<StreamGitProjectInfoWithProject?>

    @Operation(summary = "获取stream 项目下所有触发人信息")
    @GET
    @Path("/projects/members")
    fun getGitCodeProjectMembers(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "搜索用户关键字", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<StreamGitMember>?>

    @Operation(summary = "获取stream 项目所有提交信息")
    @GET
    @Path("/projects/commits")
    fun getGitCodeCommits(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @Parameter(description = "分支名称")
        @QueryParam("branch")
        branch: String?,
        @Parameter(description = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @Parameter(description = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<StreamCommitInfo>?>

    @Operation(summary = "向stream 项目中创建新文件")
    @POST
    @Path("/projects/repository/files")
    fun gitCodeCreateFile(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "创建文件内容")
        streamCreateFile: StreamCreateFileInfo
    ): Result<Boolean>

    @Operation(summary = "获取项目中的所有分支")
    @GET
    @Path("/projects/repository/branches")
    fun getGitCodeBranches(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "搜索条件，模糊匹配分支名")
        @QueryParam("search")
        search: String?,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("orderBy")
        orderBy: StreamBranchesOrder?,
        @Parameter(description = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("sort")
        sort: StreamSortAscOrDesc?
    ): Result<List<String>?>

    @Operation(summary = "获取项目中的所有分支")
    @GET
    @Path("/projects/repository/local_branches")
    fun getLocalBranches(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "搜索条件，模糊匹配分支名")
        @QueryParam("search")
        search: String?,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<String>?>

    @Operation(summary = "获取项目触发人")
    @GET
    @Path("/projects/triggers")
    fun getTriggerUser(
        @Parameter(description = "蓝盾项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<String>>
}
