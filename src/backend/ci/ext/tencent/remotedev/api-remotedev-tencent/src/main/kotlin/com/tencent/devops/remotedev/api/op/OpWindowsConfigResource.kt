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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_WINDOWS", description = "OP_WINDOWS")
@Path("/op/windowsResource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpWindowsConfigResource {
    @Operation(summary = "获取windows硬件配置")
    @GET
    @Path("/list")
    fun getWindowsResourceList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WindowsResourceTypeConfig>>

    @Operation(summary = "新增windows硬件配置")
    @POST
    @Path("/add")
    fun addWindowsResource(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Result<Boolean>

    @Operation(summary = "更新windows硬件配置")
    @PUT
    @Path("/update")
    fun updateWindowsResource(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "模板信息", required = true)
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Result<Boolean>

    @Operation(summary = "删除windows硬件配置")
    @DELETE
    @Path("/delete")
    fun deleteWindowsResource(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "获取windows区域配置")
    @GET
    @Path("/zone/list")
    fun getWindowsResourceZoneList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WindowsResourceZoneConfig>>

    @Operation(summary = "新增windows区域配置")
    @POST
    @Path("/zone/add")
    fun addWindowsZone(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Result<Boolean>

    @Operation(summary = "更新windows区域配置")
    @PUT
    @Path("/zone/update")
    fun updateWindowsZone(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "模板信息", required = true)
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Result<Boolean>

    @Operation(summary = "删除windows区域配置")
    @DELETE
    @Path("/zone/delete")
    fun deleteWindowsZone(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "创建或者更新特殊机型配置")
    @POST
    @Path("/spec/createOrUpdate")
    fun createOrUpdateSpec(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: WindowsSpecResInfo
    ): Result<Boolean>

    @Operation(summary = "删除特殊机型配额")
    @DELETE
    @Path("/spec/delete")
    fun deleteSpec(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "机型", required = true)
        @QueryParam("size")
        size: String
    ): Result<Boolean>

    @Operation(summary = "特殊机型配额列表")
    @GET
    @Path("/spec/list")
    fun fetchSpec(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
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

    @Operation(summary = "追加项目云桌面总配额")
    @PUT
    @Path("/add/{projectId}/{quota}")
    fun addProjectTotalQuota(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "配额", required = true)
        @PathParam("quota")
        quota: Int
    ): Result<Boolean>
}
