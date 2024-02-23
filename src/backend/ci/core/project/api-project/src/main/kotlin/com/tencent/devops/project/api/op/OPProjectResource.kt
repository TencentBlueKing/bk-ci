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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.OpProjectGraySetRequest
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

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
        @Parameter(name = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @Operation(summary = "更新项目信息")
    @PUT
    @Path("/")
    fun updateProject(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(name = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(name = "项目信息请求实体", required = true)
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
        @Parameter(name = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @Parameter(name = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @Parameter(name = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @Parameter(name = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @Parameter(name = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @Parameter(name = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @Parameter(name = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @Parameter(name = "偏移量", required = true)
        @QueryParam(value = "offset")
        offset: Int,
        @Parameter(name = "查询数量", required = true)
        @QueryParam(value = "limit")
        limit: Int,
        @Parameter(name = "是否灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_gray")
        grayFlag: Boolean,
        @Parameter(name = "是否灰度 true：是 false：否", required = false)
        @QueryParam(value = "is_codecc_gray")
        codeCCGrayFlag: Boolean = false,
        @Parameter(name = "是否仓库灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_repo_gray")
        repoGrayFlag: Boolean = false,
        @Parameter(name = "是否是云研发项目 true：是 false：否", required = true)
        @QueryParam(value = "is_remotedev")
        remoteDevFlag: Boolean = false,
        @Parameter(name = "运营产品ID", required = true)
        @QueryParam(value = "product_id")
        productId: Int?,
        @Context request: HttpServletRequest
    ): Result<Map<String, Any?>?>

    @Operation(summary = "灰度项目设置")
    @PUT
    @Path("/setGrayProject")
    fun setGrayProject(
        @Parameter(name = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @Operation(summary = "灰度项目设置")
    @PUT
    @Path("/codecc/setGrayProject")
    fun setCodeCCGrayProject(
        @Parameter(name = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @Operation(summary = "同步项目")
    @PUT
    @Path("/{projectId}/syn")
    fun synProject(
        @Parameter(name = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(name = "是否触发刷数据 true：是 false：否", example = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<Boolean>

    @Operation(summary = "同步项目初始化")
    @PUT
    @Path("/init/syn")
    fun synProjectInit(
        @Parameter(name = "是否触发刷数据 true：是 false：否", example = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<List<String>>

    @Operation(summary = "修改项目配置")
    @PUT
    @Path("/{projectId}/setProjectProperties")
    fun setProjectProperties(
        @Parameter(name = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(name = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(name = "项目其他配置", required = true)
        properties: ProjectProperties
    ): Result<Boolean>

    @PUT
    @Path("/enable")
    @Operation(summary = "启用或停用项目")
    fun enable(
        @Parameter(name = "待变更的新状态", required = true)
        @QueryParam("enabled")
        enabled: Boolean,
        @Parameter(name = "项目ID列表", required = true)
        englishNames: List<String>
    ): Result<Boolean>

    @PUT
    @Path("{projectId}/updateProjectProductId")
    @Operation(summary = "修改项目关联产品")
    fun updateProjectProductId(
        @Parameter(name = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(name = "产品名称", required = true)
        @QueryParam("productName")
        productName: String
    ): Result<Boolean>
}
