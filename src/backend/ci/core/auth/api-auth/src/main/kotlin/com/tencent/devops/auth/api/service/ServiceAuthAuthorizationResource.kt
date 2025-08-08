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
 *
 */

package com.tencent.devops.auth.api.service

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_RESOURCE_AUTHORIZATION", description = "权限-授权管理")
@Path("/service/auth/authorization/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthAuthorizationResource {
    @POST
    @Path("/addResourceAuthorization")
    @Operation(summary = "新增资源授权管理")
    fun addResourceAuthorization(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源授权实体", required = true)
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ): Result<Boolean>

    @GET
    @Path("/{resourceType}/{resourceCode}/getResourceAuthorization")
    @Operation(summary = "获取资源授予记录")
    fun getResourceAuthorization(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @PathParam("resourceCode")
        @Parameter(description = "资源code", required = true)
        resourceCode: String
    ): Result<ResourceAuthorizationResponse>

    @POST
    @Path("/listResourceAuthorization")
    @Operation(summary = "获取资源授权管理")
    fun listResourceAuthorization(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询条件", required = true)
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>>

    @PUT
    @Path("/batchModifyHandoverFrom")
    @Operation(summary = "批量重置资源授权人")
    fun batchModifyHandoverFrom(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "重置资源授权请求体", required = true)
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    ): Result<Boolean>
}
