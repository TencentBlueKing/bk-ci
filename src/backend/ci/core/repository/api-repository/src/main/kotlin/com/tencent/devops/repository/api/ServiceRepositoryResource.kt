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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AtomRefRepositoryInfo
import com.tencent.devops.repository.pojo.RepoPipelineRefRequest
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.commit.CommitResponse
import com.tencent.devops.repository.pojo.enums.Permission
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_REPOSITORY", description = "服务-代码库资源")
@Path("/service/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryResource {

    @Operation(summary = "关联代码库")
    @POST
    @Path("/{projectId}/")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库模型", required = true)
        repository: Repository
    ): Result<RepositoryId>

    @Operation(summary = "代码库列表")
    @GET
    @Path("/{projectId}/")
    fun list(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType?
    ): Result<List<RepositoryInfoWithPermission>>

    @Operation(summary = "获取代码库详情")
    @GET
    @Path("/{projectId}/{repositoryId}/")
    fun get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID货代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<Repository>

    @Operation(summary = "统计代码仓库个数")
    @GET
    @Path("/count")
    fun count(
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: Set<String> = setOf(),
        @Parameter(description = "代码库哈希ID", required = false)
        @QueryParam("repositoryHashId")
        repositoryHashId: String? = "",
        @Parameter(description = "代码仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType? = null,
        @Parameter(description = "代码仓库别名", required = false)
        @QueryParam("aliasName")
        aliasName: String? = ""
    ): Result<Long>

    @Operation(summary = "代码库列表")
    @GET
    @Path("/{projectId}/hasPermissionList")
    @SuppressWarnings("LongParameterList")
    fun hasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: String?,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: Permission,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null,
        @Parameter(description = "别名", required = false)
        @QueryParam("aliasName")
        aliasName: String? = null
    ): Result<Page<RepositoryInfo>>

    @Operation(summary = "获取项目代码库列表")
    @POST
    @Path("/listByProjects")
    fun listByProjects(
        projectIds: Set<String>,
        @Parameter(description = "分页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "分页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RepositoryInfo>>

    @Operation(summary = "获取项目代码库列表")
    @GET
    @Path("/projects/{projectId}/listByProject")
    fun listByProject(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType?,
        @Parameter(description = "分页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "分页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RepositoryInfo>>

    @Operation(summary = "删除代码库")
    @DELETE
    @Path("/{projectId}/{repositoryHashId}/delete")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID", required = true)
        @PathParam("repositoryHashId")
        repositoryHashId: String
    ): Result<Boolean>

    @Operation(summary = "编辑关联代码库")
    @PUT
    @Path("/{projectId}/{repositoryHashId}/")
    fun edit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID", required = true)
        @PathParam("repositoryHashId")
        repositoryHashId: String,
        @Parameter(description = "代码库模型", required = true)
        repository: Repository
    ): Result<Boolean>

    @Operation(summary = "通过hashId来获取SVN仓库信息")
    @GET
    @Path("/listByRepoHashIds")
    fun listRepoByIds(
        @Parameter(description = "仓库hashIdSet", required = true)
        @QueryParam("repositoryIds")
        repositoryIds: Set<String>
    ): Result<List<Repository>>

    @Operation(summary = "更新代码库流水线引用信息")
    @POST
    @Path("/projects/{projectId}/updatePipelineRef")
    fun updatePipelineRef(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线引用信息", required = true)
        request: RepoPipelineRefRequest
    ): Result<Boolean>

    @Operation(summary = "添加插件库的标志位")
    @POST
    @Path("/updateAtomRepoFlag")
    fun updateAtomRepoFlag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件代码库关联关系")
        atomRefRepositoryInfo: List<AtomRefRepositoryInfo>
    ): Result<Boolean>

    @Operation(summary = "根据代码库哈希ID查询GIT项目ID")
    @POST
    @Path("/git/project/retrieve")
    fun getGitProjectIdByRepositoryHashId(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库哈希ID列表", required = true)
        repositoryHashIdList: List<String>
    ): Result<List<String>>

    @Operation(summary = "更新组件代码库关联项目信息")
    @POST
    @Path("/store/project/update")
    fun updateStoreRepoProject(
        @Parameter(description = "代码库负责人")
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目ID")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "代码库ID")
        @QueryParam("repositoryId")
        repositoryId: Long
    ): Result<Boolean>

    @Operation(summary = "根据构建ID获取提交记录")
    @GET
    @Path("/{buildId}/commit/get/record")
    fun getCommit(
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<CommitResponse>>
}
