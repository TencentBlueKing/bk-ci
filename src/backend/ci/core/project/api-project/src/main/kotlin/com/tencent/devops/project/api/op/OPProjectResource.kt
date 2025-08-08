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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.OpProjectGraySetRequest
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PROJECT", description = "项目列表接口")
@Path("/op/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface OPProjectResource {

    @GET
    @Path("/")
    @Operation(summary = "查询所有项目")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @Operation(summary = "更新项目信息")
    @PUT
    @Path("/")
    fun updateProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "项目信息请求实体", required = true)
        projectInfoRequest: OpProjectUpdateInfoRequest
    ): Result<Int>

    @POST
    @Path("/updateProjectCreator")
    @Operation(summary = "修改项目创建人")
    fun updateProjectCreator(
        projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>
    ): Result<Boolean>

    @Operation(summary = "获取项目信息列表，支持筛选仓库灰度")
    @GET
    @Path("/list/project")
    fun getProjectList(
        @Parameter(description = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @Parameter(description = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @Parameter(description = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @Parameter(description = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @Parameter(description = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @Parameter(description = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @Parameter(description = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @Parameter(description = "偏移量", required = true)
        @QueryParam(value = "offset")
        offset: Int,
        @Parameter(description = "查询数量", required = true)
        @QueryParam(value = "limit")
        limit: Int,
        @Parameter(description = "是否灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_gray")
        grayFlag: Boolean,
        @Parameter(description = "是否灰度 true：是 false：否", required = false)
        @QueryParam(value = "is_codecc_gray")
        codeCCGrayFlag: Boolean = false,
        @Parameter(description = "是否仓库灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_repo_gray")
        repoGrayFlag: Boolean = false,
        @Parameter(description = "是否是云研发项目 true：是 false：否", required = true)
        @QueryParam(value = "is_remotedev")
        remoteDevFlag: Boolean = false,
        @Parameter(description = "运营产品ID", required = true)
        @QueryParam(value = "product_id")
        productId: Int?,
        @Parameter(description = "渠道", required = true)
        @QueryParam(value = "channelCode")
        channelCode: String?,
        @Context request: HttpServletRequest
    ): Result<Map<String, Any?>?>

    @Operation(summary = "灰度项目设置")
    @PUT
    @Path("/setGrayProject")
    fun setGrayProject(
        @Parameter(description = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @Operation(summary = "灰度项目设置")
    @PUT
    @Path("/codecc/setGrayProject")
    fun setCodeCCGrayProject(
        @Parameter(description = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @Operation(summary = "同步项目")
    @PUT
    @Path("/{projectId}/syn")
    fun synProject(
        @Parameter(description = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(description = "是否触发刷数据 true：是 false：否", example = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<Boolean>

    @Operation(summary = "同步项目初始化")
    @PUT
    @Path("/init/syn")
    fun synProjectInit(
        @Parameter(description = "是否触发刷数据 true：是 false：否", example = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<List<String>>

    @Operation(summary = "修改项目配置")
    @PUT
    @Path("/{projectId}/setProjectProperties")
    fun setProjectProperties(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(description = "项目其他配置", required = true)
        properties: ProjectProperties
    ): Result<Boolean>

    @PUT
    @Path("/enable")
    @Operation(summary = "启用或停用项目")
    fun enable(
        @Parameter(description = "待变更的新状态", required = true)
        @QueryParam("enabled")
        enabled: Boolean,
        @Parameter(description = "项目ID列表", required = true)
        englishNames: List<String>
    ): Result<Boolean>

    @PUT
    @Path("{projectId}/updateProjectProductId")
    @Operation(summary = "修改项目关联产品")
    fun updateProjectProductId(
        @Parameter(description = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(description = "产品名称", required = true)
        @QueryParam("productName")
        productName: String
    ): Result<Boolean>

    @GET
    @Path("/product/getOperationalProducts")
    @Operation(summary = "查询运营产品")
    fun getOperationalProducts(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OperationalProductVO>>

    @PUT
    @Path("/setDisableWhenInactiveFlag")
    fun setDisableWhenInactiveFlag(
        @Parameter(description = "项目ID列表", required = true)
        projectCodes: List<String>
    ): Result<Boolean>
}
