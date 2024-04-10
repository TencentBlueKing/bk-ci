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

package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import com.tencent.devops.remotedev.pojo.windows.TimeScope
import com.tencent.devops.remotedev.pojo.windows.UserLoginTimeResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "USER_WORKSPACE", description = "用户-工作空间")
@Path("/{apiType:user|desktop}/project_workspaces/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectWorkspaceResource {
    @Operation(summary = "创建新的工作空间实例")
    @POST
    @Path("/")
    fun createWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bkTicket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间描述", required = true)
        workspace: ProjectWorkspaceCreate
    ): Result<Boolean>

    @Operation(summary = "删除工作空间")
    @DELETE
    @Path("/")
    fun deleteWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取项目下空间列表实例列表")
    @GET
    @Path("/")
    fun getWorkspaceList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>>

    @Operation(summary = "获取项目下空间列表实例列表")
    @POST
    @Path("/search")
    fun getWorkspaceListNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<ProjectWorkspace>>

    @Operation(summary = "分配工作空间实例")
    @POST
    @Path("/assign_user")
    fun assignUser(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "工作空间描述", required = true)
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean>

    @Operation(summary = "检查是否为该项目云研发管理员")
    @GET
    @Path("/checkManager")
    fun checkManager(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "获取云研发机器状态")
    @GET
    @Path("/computerStatus")
    fun computerStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ComputerStatusResp>

    @Operation(summary = "开启工作空间")
    @POST
    @Path("/workspace/{workspaceName}/start")
    fun startWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "停止工作空间")
    @POST
    @Path("/workspace/{workspaceName}/stop")
    fun stopWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "重启工作空间")
    @POST
    @Path("/workspace/{workspaceName}/restart")
    fun restartWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "根据已存在的vm制作镜像")
    @POST
    @Path("/workspace/{workspaceName}/make_vm_image")
    fun makeImageByVm(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean>

    @Operation(summary = "获取不同时间段在线人数")
    @GET
    @Path("/userLoginTime")
    fun userLoginTime(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "timeScope", required = false)
        @QueryParam("timeScope")
        timeScope: TimeScope? = TimeScope.HOUR
    ): Result<UserLoginTimeResp>

    @Operation(summary = "导出项目下空间列表实例列表")
    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportWorkspaceList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Response

    @Operation(summary = "重装工作空间系统")
    @POST
    @Path("/workspace/{workspaceName}/rebuild")
    fun reBuildWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "请求报文", required = true)
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean>

    @Operation(summary = "特殊机型配额列表")
    @GET
    @Path("/spec/list")
    fun fetchSpec(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = false)
        @PathParam("projectId")
        projectId: String?,
        @Parameter(description = "机型", required = false)
        @QueryParam("machineType")
        machineType: String?,
        @Parameter(description = "第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<WindowsSpecResInfo>>
}
