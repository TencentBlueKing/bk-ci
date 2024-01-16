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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import com.tencent.devops.remotedev.pojo.software.UserSoftwareInstalledRecord
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_SOFTWARE_MANAGE", description = "用户-软件管理")
@Path("/user/software")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSoftwareManageResource {

    @Operation(summary = "获取项目下软件列表")
    @GET
    @Path("/list")
    fun getProjectSoftwareList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("project_id")
        projectId: String
    ): Result<List<ProjectSoftware>>

    @Operation(summary = "导入软件到项目列表")
    @POST
    @Path("/import")
    fun importSoftwareToProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "导入软件信息", required = true)
        software: ProjectSoftware
    ): Result<Boolean>

    @Operation(summary = "安装软件到用户")
    @POST
    @Path("/install")
    fun installSoftwareToUser(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "软件安装列表", required = true)
        softwareList: List<UserSoftware>
    ): Result<Boolean>

    @Operation(summary = "获取用户的安装软件记录")
    @GET
    @Path("/install/history")
    fun getUserSoftwareInstalledRecord(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("project_id")
        projectId: String,
        @Parameter(description = "用户", required = false)
        @QueryParam("user")
        user: String?,
        @Parameter(description = "云桌面", required = false)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "状态", required = false)
        @QueryParam("status")
        status: SoftwareInstallStatus?
    ): Result<List<UserSoftwareInstalledRecord>>

    @Operation(summary = "获取行云软件分组")
    @GET
    @Path("/group")
    fun getSoftwareGroupInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Any>
}
