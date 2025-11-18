/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WorkspaceGroup
import com.tencent.devops.remotedev.pojo.WorkspaceGroupAssignment
import com.tencent.devops.remotedev.pojo.WorkspaceGroupCreate
import com.tencent.devops.remotedev.pojo.WorkspaceGroupItem
import com.tencent.devops.remotedev.pojo.WorkspaceGroupUpdate
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_WORKSPACE_GROUP", description = "用户-云桌面分组")
@Path("/user/workspaceGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserWorkspaceGroupResource {

    @Operation(summary = "获取项目下云桌面分组列表")
    @GET
    @Path("/groups")
    fun getGroups(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<WorkspaceGroup>>

    @Operation(summary = "创建云桌面分组")
    @POST
    @Path("/groups")
    fun addGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        group: WorkspaceGroupCreate
    ): Result<Boolean>

    @Operation(summary = "更新云桌面分组")
    @PUT
    @Path("/groups")
    fun updateGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        group: WorkspaceGroupUpdate
    ): Result<Boolean>

    @Operation(summary = "删除云桌面分组")
    @DELETE
    @Path("/groups")
    fun deleteGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "分组ID", required = true)
        @QueryParam("groupId")
        groupId: Long
    ): Result<Boolean>

    @Operation(summary = "将工作空间加入分组")
    @POST
    @Path("/groups/workspaces/add")
    fun addWorkspaces(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "分组ID", required = true)
        @QueryParam("groupId")
        groupId: Long,
        assignment: WorkspaceGroupAssignment
    ): Result<Boolean>

    @Operation(summary = "将工作空间从分组移除")
    @DELETE
    @Path("/groups/workspaces/remove")
    fun removeWorkspace(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "分组ID", required = true)
        @QueryParam("groupId")
        groupId: Long,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "分页获取某分组下工作空间列表")
    @GET
    @Path("/groups/workspaces")
    fun listWorkspaces(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "分组ID", required = true)
        @QueryParam("groupId")
        groupId: Long,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        @DefaultValue("1")
        page: Int = 1,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        @DefaultValue("20")
        pageSize: Int = 20
    ): Result<Page<WorkspaceGroupItem>>
}
