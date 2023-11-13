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
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.windows.TimeScope
import com.tencent.devops.remotedev.pojo.windows.UserLoginTimeResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

@Api(tags = ["USER_WORKSPACE"], description = "用户-工作空间")
@Path("/{apiType:user|desktop}/project_workspaces/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectWorkspaceResource {
    @ApiOperation("创建新的工作空间实例")
    @POST
    @Path("/")
    fun createWorkspace(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "bkTicket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间描述", required = true)
        workspace: ProjectWorkspaceCreate
    ): Result<Boolean>

    @ApiOperation("删除工作空间")
    @DELETE
    @Path("/")
    fun deleteWorkspace(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @ApiOperation("获取项目下空间列表实例列表")
    @GET
    @Path("/")
    fun getWorkspaceList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>>

    @ApiOperation("分配工作空间实例")
    @POST
    @Path("/assign_user")
    fun assignUser(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @ApiParam("工作空间描述", required = true)
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean>

    @ApiOperation("检查是否为该项目云研发管理员")
    @GET
    @Path("/checkManager")
    fun checkManager(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("获取云研发机器状态")
    @GET
    @Path("/computerStatus")
    fun computerStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ComputerStatusResp>

    @ApiOperation("开启工作空间")
    @POST
    @Path("/workspace/{workspaceName}/start")
    fun startWorkspace(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @ApiOperation("停止工作空间")
    @POST
    @Path("/workspace/{workspaceName}/stop")
    fun stopWorkspace(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @ApiOperation("重启工作空间")
    @POST
    @Path("/workspace/{workspaceName}/restart")
    fun restartWorkspace(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @ApiOperation("根据已存在的vm制作镜像")
    @POST
    @Path("/workspace/{workspaceName}/make_vm_image")
    fun makeImageByVm(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("工作空间名称", required = true)
        @PathParam("workspaceName")
        workspaceName: String,
        @ApiParam("请求报文", required = true)
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean>

    @ApiOperation("获取不同时间段在线人数")
    @GET
    @Path("/userLoginTime")
    fun userLoginTime(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "timeScope", required = false)
        @QueryParam("timeScope")
        timeScope: TimeScope? = TimeScope.HOUR
    ): Result<UserLoginTimeResp>

    @ApiOperation("导出项目下空间列表实例列表")
    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportWorkspaceList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Response
}
