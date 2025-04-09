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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.process.pojo.BuildFormRepositoryValue
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.common.pipeline.pojo.BuildEnvParameters
import com.tencent.devops.common.pipeline.pojo.BuildParameterGroup
import com.tencent.devops.process.pojo.pipeline.PipelineBuildParamFormProp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_BUILD_PARAMETERS", description = "用户-构建环境参数")
@Path("/user/buildParam")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface UserBuildParametersResource {

    @Operation(summary = "获取构建的公共参数")
    @GET
    @Path("/")
    fun getCommonBuildParams(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<BuildEnvParameters>>

    @Operation(summary = "获取构建的公共参数新接口(ci.xxx)")
    @POST
    @Path("/common")
    fun getCommonParams(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<BuildParameterGroup>>

    @Operation(summary = "获取构建的触发器参数")
    @POST
    @Path("/trigger")
    fun getTriggerParams(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "需要请求的触发器插件标识", required = true)
        atomCodeList: List<String?>
    ): Result<List<BuildParameterGroup>>

    @Operation(summary = "构建表单查询代码库别名列表")
    @GET
    @Path("/repository/{projectId}/aliasName")
    fun listRepositoryAliasName(
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
        @Parameter(description = "别名", required = false)
        @QueryParam("aliasName")
        aliasName: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<BuildFormValue>>

    @Operation(summary = "构建表单查询代码库hashId列表")
    @GET
    @Path("/repository/{projectId}/hashId")
    fun listRepositoryHashId(
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
        @Parameter(description = "别名", required = false)
        @QueryParam("aliasName")
        aliasName: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<BuildFormRepositoryValue>>

    @Operation(summary = "构建表单查询流水线列表")
    @GET
    @Path("/{projectId}/subPipeline")
    fun listPermissionPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: com.tencent.devops.process.pojo.Permission,
        @Parameter(description = "排除流水线ID", required = false, example = "")
        @QueryParam("excludePipelineId")
        excludePipelineId: String?,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("pipelineName")
        pipelineName: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<BuildFormValue>>

    @Operation(summary = "构建表单查询git分支")
    @GET
    @Path("/{projectId}/{repositoryId}/gitRefs")
    fun listGitRefs(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<BuildFormValue>>

    @Operation(summary = "构建表单查询分支/Tag变量")
    @GET
    @Path("/{projectId}/repository/refs")
    fun listRepoRefs(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id", required = true)
        @QueryParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<BuildFormValue>>

    @Operation(summary = "查询流水线启动参数信息[下拉选填充]")
    @GET
    @Path("/{projectId}/{pipelineId}/buildParamFormProp")
    fun buildParamFormProp(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否包含常量", required = false, example = "")
        @QueryParam("includeConst")
        includeConst: Boolean? = true,
        @Parameter(description = "是否包含非入参", required = false, example = "")
        @QueryParam("includeNotRequired")
        includeNotRequired: Boolean? = true,
        @Parameter(description = "当前流水线版本", required = false, example = "")
        @QueryParam("version")
        version: Int? = null
    ): Result<List<PipelineBuildParamFormProp>>
}
