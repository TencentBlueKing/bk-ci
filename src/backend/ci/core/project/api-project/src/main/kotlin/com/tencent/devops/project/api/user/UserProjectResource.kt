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

package com.tencent.devops.project.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import com.tencent.devops.project.pojo.ProjectCollation
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectSortType
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PROJECT", description = "项目列表接口")
@Path("/{apiType:user|desktop}/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface UserProjectResource {

    @GET
    @Path("/")
    @Operation(summary = "查询所有项目")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @Parameter(description = "是否启用", required = false)
        @QueryParam("enabled")
        enabled: Boolean?,
        @Parameter(description = "是否拉取未审批通过的项目，若为true，会拉取审批[未通过+通过]的项目", required = false)
        @QueryParam("unApproved")
        unApproved: Boolean?,
        @Parameter(description = "项目排序", required = false, example = "PROJECT_NAME")
        @QueryParam("sortType")
        sortType: ProjectSortType?,
        @Parameter(description = "排序规则", required = false)
        @QueryParam("collation")
        collation: ProjectCollation?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/listProjectsForApply")
    @Operation(summary = "查询项目--用于权限申请界面")
    fun listProjectsForApply(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "access_token", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @Parameter(description = "项目名", required = false)
        @QueryParam("projectName")
        projectName: String? = null,
        @Parameter(description = "项目ID英文名标识", required = true)
        @QueryParam("english_name")
        projectId: String? = null,
        @Parameter(description = "页目", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数目", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Pagination<ProjectByConditionDTO>>

    @GET
    @Path("/{english_name}")
    @Operation(summary = "获取项目信息，为空抛异常")
    fun get(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectVO>

    @GET
    @Path("/{english_name}/show")
    @Operation(summary = "前端获取项目详情,有project_view权限校验")
    fun show(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectVO>

    @GET
    @Path("/{english_name}/diff")
    @Operation(summary = "获取项目编辑信息对比")
    fun diff(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectDiffVO>

    @GET
    @Path("/{english_name}/containEmpty")
    @Operation(summary = "获取项目信息为空返回空对象")
    fun getContainEmpty(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("english_name")
        projectId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectVO?>

    @POST
    @Path("/")
    @Operation(summary = "创建项目")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<Boolean>

    @PUT
    @Path("/{project_id}")
    @Operation(summary = "修改项目")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("project_id")
        projectId: String,
        @Parameter(description = "项目信息", required = true)
        projectUpdateInfo: ProjectUpdateInfo,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<Boolean>

    @PUT
    @Path("/{project_id}/enable")
    @Operation(summary = "启用或停用项目")
    fun enable(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("project_id")
        projectId: String,
        @Parameter(description = "待变更的新状态", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>

    @PUT
    @Path("/{english_name}/logo")
    @Operation(summary = "更改项目logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateLogo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目英文名", required = true)
        @PathParam("english_name")
        englishName: String,
        @Parameter(description = "文件", required = true)
        @FormDataParam("logo")
        inputStream: InputStream,
        @FormDataParam("logo")
        disposition: FormDataContentDisposition,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectLogo>

    @POST
    @Path("/upload/logo")
    @Operation(summary = "上传logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadLogo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "文件", required = true)
        @FormDataParam("logo")
        inputStream: InputStream,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<String>

    @PUT
    @Path("/{validateType}/names/validate")
    @Operation(summary = "校验项目名称和项目英文名")
    fun validate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "校验的是项目名称或者项目英文名")
        @PathParam("validateType")
        validateType: ProjectValidateType,
        @Parameter(description = "项目名称或者项目英文名")
        @QueryParam("name")
        name: String,
        @Parameter(description = "项目ID")
        @QueryParam("english_name")
        projectId: String?
    ): Result<Boolean>

    @Operation(summary = "是否拥有创建项目")
    @Path("/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @Operation(summary = "是否拥有某实例的某action的权限")
    @Path("/{projectId}/hasPermission/{permission}")
    @GET
    fun hasPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "权限action", required = true)
        @PathParam("permission")
        permission: AuthPermission
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/users/{userId}/verify")
    @Operation(summary = " 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @Parameter(description = "accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @Operation(summary = "取消创建项目")
    @Path("/{project_id}/cancelCreateProject")
    @PUT
    fun cancelCreateProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("project_id")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "取消更新项目")
    @Path("/{project_id}/cancelUpdateProject")
    @PUT
    fun cancelUpdateProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("project_id")
        projectId: String
    ): Result<Boolean>

    @GET
    @Path("/product/getOperationalProducts")
    @Operation(summary = "查询运营产品")
    fun getOperationalProducts(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OperationalProductVO>>

    @GET
    @Path("/{english_name}/remindUserOfRelatedProduct")
    @Operation(summary = "提醒用户关联运营产品")
    fun remindUserOfRelatedProduct(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目英文名称", required = true)
        @PathParam("english_name")
        englishName: String
    ): Result<Boolean>

    @GET
    @Path("/product/getOperationalProductsByBgName/{bgName}")
    @Operation(summary = "根据BG查询运营产品")
    fun getOperationalProductsByBgName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "bg名称", required = true)
        @PathParam("bgName")
        bgName: String
    ): Result<List<OperationalProductVO>>

    @Operation(summary = "获取项目级流水线方言, 流水线编辑/修改时调用")
    @GET
    @Path("{projectId}/pipelineDialect")
    fun getPipelineDialect(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<String>
}
