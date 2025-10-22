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

package com.tencent.devops.auth.api.sync

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_SYNC", description = "权限-同步IAM")
@Path("/op/auth/resource/group/sync/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAuthResourceGroupSyncResource {

    @POST
    @Path("/syncByCondition")
    @Operation(summary = "按条件同步组和成员")
    fun syncByCondition(
        @Parameter(description = "按条件迁移项目实体", required = true)
        projectConditionDTO: ProjectConditionDTO
    ): Result<Boolean>

    @POST
    @Path("/syncGroupMemberExpiredTime")
    @Operation(summary = "按条件同步组成员过期时间")
    fun syncGroupMemberExpiredTime(
        @Parameter(description = "按条件迁移项目实体", required = true)
        projectConditionDTO: ProjectConditionDTO
    ): Result<Boolean>

    @POST
    @Path("/batchSyncGroupAndMember")
    @Operation(summary = "批量同步所有用户组和成员")
    fun batchSyncGroupAndMember(
        @Parameter(description = "项目ID列表", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @POST
    @Path("/batchSyncProjectGroup")
    @Operation(summary = "批量同步项目下用户组")
    fun batchSyncProjectGroup(
        @Parameter(description = "项目ID列表", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @POST
    @Path("/batchSyncAllMember")
    @Operation(summary = "同步所有成员")
    fun batchSyncAllMember(
        @Parameter(description = "项目ID列表", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @POST
    @Path("/{projectId}/{resourceType}/{resourceCode}/syncResourceMember")
    @Operation(summary = "同步资源下用户组")
    fun syncResourceMember(
        @Parameter(description = "项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam(value = "resourceType")
        resourceType: String,
        @Parameter(description = "资源ID", required = true)
        @PathParam(value = "resourceCode")
        resourceCode: String
    ): Result<Boolean>

    @POST
    @Path("/{projectId}/fixResourceGroupMember")
    @Operation(summary = "修复用户组成员表")
    fun fixResourceGroupMember(
        @Parameter(description = "项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String
    ): Result<Boolean>

    @POST
    @Path("/syncIamGroupMembersOfApply")
    @Operation(summary = "同步iam组成员--用户申请加入")
    fun syncIamGroupMembersOfApply(): Result<Boolean>
}
