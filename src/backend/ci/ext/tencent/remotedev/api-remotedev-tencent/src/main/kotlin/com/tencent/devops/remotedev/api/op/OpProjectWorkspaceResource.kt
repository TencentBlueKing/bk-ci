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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["OP_PROJECT_WORKSPACE"], description = "OP_PROJECT_WORKSPACE")
@Path("/op/project/workspace")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectWorkspaceResource {

    @ApiOperation("分配云桌面")
    @POST
    @Path("/assign")
    fun assignWorkspace(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "分配数据")
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean>

    @ApiOperation("获取项目下空间列表实例列表")
    @POST
    @Path("/list")
    fun getProjectWorkspaceList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("查询参数")
        data: ProjectWorkspaceFetchData
    ): Result<Page<ProjectWorkspace>>

    @ApiOperation("批量获取指定项目的云桌面的云研发管理员和拥有者")
    @POST
    @Path("/fetchOwnerAndAdmin")
    fun fetchOwnerAndAdmin(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("获取数据")
        data: FetchOwnerAndAdminData
    ): Result<Set<String>>

    @ApiOperation("修改云研发机器在 CMDB 的属性")
    @POST
    @Path("/updateCCHost")
    fun updateCCHost(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("修改数据")
        data: OpUpdateCCHostData
    ): Result<Boolean>

    @ApiOperation("刷新 codeproxy 数据")
    @POST
    @Path("/refreshCodeProxy")
    fun refreshCodeProxy(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId")
        projectId: String
    )

    @ApiOperation("导出实例页面查询结果")
    @POST
    @Path("/list/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportProjectWorkspaceList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("查询参数")
        data: ProjectWorkspaceFetchData
    ): Response

    @ApiOperation("云桌面通知")
    @POST
    @Path("/notify")
    fun notify(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "通知信息", required = true)
        notifyData: WorkspaceNotifyData
    ): Result<Boolean>
}
