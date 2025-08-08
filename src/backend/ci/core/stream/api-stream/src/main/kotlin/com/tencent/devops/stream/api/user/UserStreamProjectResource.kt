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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.stream.pojo.StreamProjectCIInfo
import com.tencent.devops.stream.pojo.enums.StreamProjectType
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STREAM_PROJECT", description = "user-项目资源")
@Path("/user/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamProjectResource {

    @Operation(summary = "获取Git项目与STREAM关联列表")
    @GET
    @Path("/{type}/list")
    fun getProjects(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目列表类型", required = false)
        @PathParam("type")
        type: StreamProjectType?,
        @Parameter(description = "搜索条件，模糊匹配path,name", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "10")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "排序条件", required = false)
        @QueryParam("orderBy")
        orderBy: StreamProjectsOrder?,
        @Parameter(description = "排序类型", required = false)
        @QueryParam("sort")
        sort: StreamSortAscOrDesc?
    ): Pagination<StreamProjectCIInfo>

    @Operation(summary = "获取用户最近访问的项目")
    @GET
    @Path("/history")
    fun getProjectsHistory(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "多少条记录", required = false, example = "4")
        @QueryParam("pageSize")
        size: Long?
    ): Result<List<StreamProjectCIInfo>>

    @Operation(summary = "获取项目信息")
    @GET
    @Path("/{english_name}")
    fun getProjectInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String
    ): Result<ProjectVO?>

    @Operation(summary = "更新项目组织架构和归属")
    @PUT
    @Path("/{english_name}/organization")
    fun updateProjectOrganization(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String,
        @Parameter(description = "产品ID", required = true)
        @QueryParam("productId")
        productId: Int,
        @Parameter(description = "产品名称", required = true)
        @QueryParam("productName")
        productName: String,
        @Parameter(description = "项目组织", required = true)
        organization: ProjectOrganizationInfo
    ): Result<Boolean>
}
