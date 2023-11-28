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

package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_WORKSPACE"], description = "OP_WORKSPACE")
@Path("/op/workspace")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpWorkspaceResource {

    @ApiOperation("分享工作空间")
    @POST
    @Path("/share/add")
    fun shareWorkspace(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工作空间共享", required = true)
        workspaceShared: WorkspaceSharedOpUse
    ): Result<Boolean>

    @ApiOperation("分享或删除工作空间")
    @POST
    @Path("/share/update")
    fun shareWorkspace4OP(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工作空间共享信息", required = true)
        shareWorkspace: ShareWorkspace
    ): Result<Boolean>

    @ApiOperation("获取分享工作空间列表")
    @GET
    @Path("/share/list")
    fun getShareWorkspace(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String?
    ): Result<List<WorkspaceShared>>

    @ApiOperation("删除分享工作空间")
    @DELETE
    @Path("/share/delete")
    fun deleteShareWorkspace(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "id", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>

    @ApiOperation("转移工作空间detail数据到db")
    @GET
    @Path("/detail/move")
    fun moveWorkspaceDetail(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @ApiOperation("变更工作空间状态")
    @GET
    @Path("/status_change")
    fun updateStatus(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @QueryParam("workspaceStatus")
        workspaceStatus: WorkspaceStatus
    ): Result<Boolean>

    @ApiOperation("通过已有cgsIp实例创建workspace记录")
    @POST
    @Path("/create_win_workspace_by_vm")
    fun createWinWorkspaceByVm(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "老workspace记录，可以为空，如果填写将会做清理", required = true)
        @QueryParam("oldWorkspaceName")
        oldWorkspaceName: String?,
        @ApiParam(value = "项目ID，可以为空，如果填写就是团队空间，否则个人空间", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam(value = "机器uid", required = true)
        @QueryParam("uid")
        uid: String
    ): Result<Boolean>
}
