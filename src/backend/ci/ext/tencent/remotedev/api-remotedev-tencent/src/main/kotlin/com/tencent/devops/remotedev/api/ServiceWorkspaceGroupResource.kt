/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_WORKSPACE_GROUP", description = "服务-云桌面分组")
@Path("/service/workspaceGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceWorkspaceGroupResource {

    @Operation(summary = "根据分组ID列表获取工作空间名称列表")
    @GET
    @Path("/workspaces/byGroups")
    fun getWorkspacesByGroups(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "分组ID列表", required = true)
        @QueryParam("groupIds")
        groupIds: List<Long>
    ): Result<List<String>>

    @Operation(summary = "根据工作空间名称获取所属分组ID列表")
    @GET
    @Path("/groups/byWorkspace")
    fun getGroupsByWorkspace(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<List<Long>>
}
