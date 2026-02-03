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


package com.tencent.devops.auth.api.op

import com.tencent.devops.auth.pojo.request.ActionCreateRequest
import com.tencent.devops.auth.pojo.request.FullResourceTypeConfigCreateRequest
import com.tencent.devops.auth.pojo.request.ProjectGroupConfigUpdateRequest
import com.tencent.devops.auth.pojo.request.ResourceGroupConfigCreateRequest
import com.tencent.devops.auth.pojo.request.ResourceTypeCreateRequest
import com.tencent.devops.auth.pojo.vo.ActionVO
import com.tencent.devops.auth.pojo.vo.ResourceGroupConfigVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeVO
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AUTH_RESOURCE_TYPE_CONFIG", description = "OP-资源类型配置管理")
@Path("/op/auth/resourceTypeConfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions")
interface OpAuthResourceTypeConfigResource {

    // ==================== 资源类型管理 ====================

    @GET
    @Path("/resourceTypes")
    @Operation(summary = "获取所有资源类型列表")
    fun listResourceTypes(): Result<List<ResourceTypeVO>>

    @GET
    @Path("/resourceTypes/{resourceType}")
    @Operation(summary = "获取单个资源类型详情")
    fun getResourceType(
        @Parameter(description = "资源类型代码", required = true)
        @PathParam("resourceType")
        resourceType: String
    ): Result<ResourceTypeVO?>

    @POST
    @Path("/resourceTypes")
    @Operation(summary = "创建资源类型")
    fun createResourceType(
        @Parameter(description = "资源类型创建请求", required = true)
        request: ResourceTypeCreateRequest
    ): Result<Int>

    @DELETE
    @Path("/resourceTypes/{resourceType}")
    @Operation(summary = "删除资源类型（软删除）")
    fun deleteResourceType(
        @Parameter(description = "资源类型代码", required = true)
        @PathParam("resourceType")
        resourceType: String
    ): Result<Boolean>

    // ==================== 操作管理 ====================

    @GET
    @Path("/actions")
    @Operation(summary = "获取操作列表")
    fun listActions(
        @Parameter(description = "资源类型代码", required = false)
        @QueryParam("resourceType")
        resourceType: String?
    ): Result<List<ActionVO>>

    @GET
    @Path("/actions/{action}")
    @Operation(summary = "获取单个操作详情")
    fun getAction(
        @Parameter(description = "操作代码", required = true)
        @PathParam("action")
        action: String
    ): Result<ActionVO?>

    @POST
    @Path("/actions")
    @Operation(summary = "创建操作")
    fun createAction(
        @Parameter(description = "操作创建请求", required = true)
        request: ActionCreateRequest
    ): Result<Boolean>

    @POST
    @Path("/actions/batch")
    @Operation(summary = "批量创建操作")
    fun batchCreateActions(
        @Parameter(description = "操作创建请求列表", required = true)
        requests: List<ActionCreateRequest>
    ): Result<Int>

    @DELETE
    @Path("/actions/{action}")
    @Operation(summary = "删除操作（软删除）")
    fun deleteAction(
        @Parameter(description = "操作代码", required = true)
        @PathParam("action")
        action: String
    ): Result<Boolean>

    // ==================== 资源用户组配置管理 ====================

    @GET
    @Path("/groupConfigs")
    @Operation(summary = "获取用户组配置列表")
    fun listGroupConfigs(
        @Parameter(description = "资源类型代码", required = false)
        @QueryParam("resourceType")
        resourceType: String?
    ): Result<List<ResourceGroupConfigVO>>

    @GET
    @Path("/groupConfigs/{id}")
    @Operation(summary = "获取单个用户组配置详情")
    fun getGroupConfig(
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<ResourceGroupConfigVO?>

    @POST
    @Path("/groupConfigs")
    @Operation(summary = "创建资源用户组配置")
    fun createGroupConfig(
        @Parameter(description = "用户组配置创建请求", required = true)
        request: ResourceGroupConfigCreateRequest
    ): Result<Long>

    @POST
    @Path("/groupConfigs/batch")
    @Operation(summary = "批量创建资源用户组配置")
    fun batchCreateGroupConfigs(
        @Parameter(description = "用户组配置创建请求列表", required = true)
        requests: List<ResourceGroupConfigCreateRequest>
    ): Result<Int>

    @PUT
    @Path("/groupConfigs/{id}/appendActions")
    @Operation(summary = "追加新的资源类型权限块到用户组配置（新增一个完整的权限块）")
    fun appendActionsToGroupConfig(
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "要追加的资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "要追加的操作列表", required = true)
        actions: List<String>
    ): Result<Boolean>

    @PUT
    @Path("/groupConfigs/{id}/appendActionsToExistingScope")
    @Operation(summary = "在已有的资源类型权限块中追加 actions（不新增权限块，只追加 action）")
    fun appendActionsToExistingScope(
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "目标资源类型（用于定位已存在的权限块）", required = true)
        @QueryParam("targetResourceType")
        targetResourceType: String,
        @Parameter(description = "要追加的操作列表", required = true)
        actions: List<String>
    ): Result<Boolean>

    @PUT
    @Path("/groupConfigs/{id}/smartAppendActions")
    @Operation(summary = "智能追加 actions（如果权限块存在则追加，不存在则新建）")
    fun smartAppendActions(
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "要追加的操作列表", required = true)
        actions: List<String>
    ): Result<Boolean>

    @POST
    @Path("/groupConfigs/batchAppendActions")
    @Operation(summary = "批量追加操作到项目级用户组配置（新增权限块方式）")
    fun batchAppendActionsToProjectGroups(
        @Parameter(description = "批量更新请求列表", required = true)
        requests: List<ProjectGroupConfigUpdateRequest>
    ): Result<Int>

    @POST
    @Path("/groupConfigs/batchSmartAppendActions")
    @Operation(summary = "批量智能追加 actions（如果权限块存在则追加，不存在则新建）")
    fun batchSmartAppendActions(
        @Parameter(description = "批量更新请求列表", required = true)
        requests: List<ProjectGroupConfigUpdateRequest>
    ): Result<Int>

    @DELETE
    @Path("/groupConfigs/{id}")
    @Operation(summary = "删除用户组配置")
    fun deleteGroupConfig(
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    // ==================== 便捷接口：一键创建完整资源类型配置 ====================

    @POST
    @Path("/resourceTypes/full")
    @Operation(summary = "一键创建完整资源类型配置（资源类型+操作+用户组配置）")
    fun createFullResourceTypeConfig(
        @Parameter(description = "完整资源类型配置创建请求", required = true)
        request: FullResourceTypeConfigCreateRequest
    ): Result<Boolean>
}
