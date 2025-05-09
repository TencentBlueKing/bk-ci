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

package com.tencent.devops.repository.api.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.Perm
import com.tencent.devops.scm.api.pojo.Reference
import com.tencent.devops.scm.api.pojo.repository.ScmServerRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM_REPOSITORY_API", description = "服务-代码源-仓库API")
@Path("/service/scm/repository/api/{projectId}/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmRepositoryApiResource {

    @Operation(summary = "获取服务端仓库信息")
    @POST
    @Path("/getServerRepository")
    fun getServerRepository(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository
    ): Result<ScmServerRepository>

    @Operation(summary = "获取用户在代码库中拥有的权限")
    @POST
    @Path("/findPerm")
    fun findPerm(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户名", required = true)
        @QueryParam("username")
        username: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository
    ): Result<Perm>

    @Operation(summary = "获取仓库分支信息")
    @POST
    @Path("/findBranches")
    fun findBranches(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<Reference>>

    @Operation(summary = "获取目标分支信息")
    @POST
    @Path("/getBranch")
    fun getBranch(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("branch")
        branch: String
    ): Result<Reference?>

    @Operation(summary = "获取目标Tag信息")
    @POST
    @Path("/findTags")
    fun findTags(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<Reference>>
}
