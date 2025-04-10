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

package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.OrgInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.PluginDetailsDisplayOrder
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "SERVICE_PROJECT", description = "项目列表接口")
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectResource {

    @GET
    @Path("/")
    @Operation(summary = "查询当前用户有权限的项目列表")
    fun list(
        @Parameter(description = "用户ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "userId", required = false)
        @QueryParam("productIds")
        productIds: String? = null
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getAllProject")
    @Operation(summary = "查询所有项目")
    fun getAllProject(): Result<List<ProjectVO>>

    @POST
    @Path("/listProjectsByCondition/{offset}/{limit}")
    @Operation(summary = "根据条件查询项目")
    fun listProjectsByCondition(
        @Parameter(description = "条件迁移项目实体", required = false)
        projectConditionDTO: ProjectConditionDTO,
        @Parameter(description = "limit", required = true)
        @PathParam("limit")
        limit: Int,
        @Parameter(description = "offset", required = true)
        @PathParam("offset")
        offset: Int
    ): Result<List<ProjectByConditionDTO>>

    @POST
    @Path("/")
    @Operation(summary = "查询指定项目，不包括被禁用的项目")
    fun listByProjectCode(
        @Parameter(description = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @POST
    @Path("/listOnlyByProjectCode")
    @Operation(summary = "查询指定项目，包括被禁用的项目")
    fun listOnlyByProjectCode(
        @Parameter(description = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @POST
    @Path("/listByProjectCodes")
    @Operation(summary = "查询指定项目")
    fun listByProjectCodeList(
        @Parameter(description = "项目id", required = true)
        projectCodes: List<String>
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByUser")
    @Operation(summary = "查询所有项目")
    fun getProjectByUser(
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        userName: String
    ): Result<List<ProjectVO>>

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

    @GET
    @Path("/getNameByCode")
    @Operation(summary = "根据项目Code获取对应的名称")
    fun getNameByCode(
        @Parameter(description = "projectCodes，多个以英文逗号分隔", required = true)
        @QueryParam("projectCodes")
        projectCodes: String
    ): Result<HashMap<String, String>>

    @GET
    @Path("/{projectId}")
    @Operation(summary = "查询指定EN项目")
    fun get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        englishName: String
    ): Result<ProjectVO?>

    @POST
    @Path("/create")
    @Operation(summary = "创建项目")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @Parameter(description = "accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null
    ): Result<Boolean>

    @POST
    @Path("/create/ext/system")
    @Operation(summary = "创建扩展系统项目")
    fun createExtSystem(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目信息")
        projectInfo: ProjectCreateInfo,
        @QueryParam("needAuth")
        @Parameter(description = "是否需要权限")
        needAuth: Boolean,
        @QueryParam("needAuth")
        @Parameter(description = "是否需要校验")
        needValidate: Boolean,
        @QueryParam("projectChanel")
        channel: ProjectChannelCode
    ): Result<ProjectVO?>

    @PUT
    @Path("/{projectId}")
    @Operation(summary = "修改项目")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "项目信息", required = true)
        projectUpdateInfo: ProjectUpdateInfo,
        @Parameter(description = "accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null
    ): Result<Boolean>

    @PUT
    @Path("/{projectCode}/projectName")
    fun updateProjectName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectCode}/properties")
    fun updateProjectProperties(
        @Parameter(description = "项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "项目名称", required = true)
        properties: ProjectProperties
    ): Result<Boolean>

    @GET
    @Path("projectNames/{projectName}")
    fun getProjectByName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectName", required = true)
        @PathParam("projectName")
        projectName: String
    ): Result<ProjectVO?>

    @PUT
    @Path("/{validateType}/names/validate")
    @Operation(summary = "校验项目名称和项目英文名")
    fun validate(
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

    @POST
    @Path("/{projectId}/orgcheck")
    @Operation(summary = "是否是组织下的项目")
    fun isOrgProject(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "orgInfo", required = true)
        orgInfos: OrgInfo
    ): Result<Boolean>

    @GET
    @Path("/getMinId")
    @Operation(summary = "查询最小项目ID")
    fun getMinId(): Result<Long>

    @GET
    @Path("/getMaxId")
    @Operation(summary = "查询最大项目ID")
    fun getMaxId(): Result<Long>

    @GET
    @Path("/getProjectListById")
    @Operation(summary = "根据ID查询项目列表")
    fun getProjectListById(
        @Parameter(description = "最小项目ID", required = true)
        @QueryParam("minId")
        minId: Long,
        @Parameter(description = "最大项目ID", required = true)
        @QueryParam("maxId")
        maxId: Long
    ): Result<List<ProjectBaseInfo>>

    @Operation(summary = "查看灰度项目列表")
    @GET
    @Path("/listSecrecyProject")
    fun listSecrecyProject(): Result<Set<String>?>

    @Operation(summary = "为项目添加成员")
    @POST
    @Path("/{projectId}/createUser")
    fun createProjectUser(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserInfo
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

    @PUT
    @Path("/{projectId}/updateProjectSubjectScopes")
    @Operation(summary = "修改项目最大可授权范围")
    fun updateProjectSubjectScopes(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "ke", required = true)
        subjectScopes: List<SubjectScopeInfo>
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
        productName: String? = null,
        @Parameter(description = "产品ID", required = true)
        @QueryParam("productId")
        productId: Int? = null
    ): Result<Boolean>

    @PUT
    @Path("{projectId}/updateOrganizationByEnglishName")
    @Operation(summary = "修改项目组织架构")
    fun updateOrganizationByEnglishName(
        @Parameter(description = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @Parameter(description = "产品名称", required = true)
        projectOrganizationInfo: ProjectOrganizationInfo
    ): Result<Boolean>

    @GET
    @Path("/getProjectListByProductId")
    @Operation(summary = "根据运营产品ID获取项目列表接口")
    fun getProjectListByProductId(
        @Parameter(description = "产品ID", required = true)
        @QueryParam("productId")
        productId: Int
    ): Result<List<ProjectBaseInfo>>

    @GET
    @Path("/getExistedEnglishName")
    @Operation(summary = "传入项目ID列表，返回其中存在的项目ID列表接口")
    fun getExistedEnglishName(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("englishName")
        englishName: List<String>
    ): Result<List<String>?>

    @PUT
    @Path("{projectId}/updatePluginDetailsDisplay")
    @Operation(summary = "更新插件展示顺序")
    fun updatePluginDetailsDisplay(
        @Parameter(description = "项目Code", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "插件展示顺序", required = true)
        pluginDetailsDisplayOrder: List<PluginDetailsDisplayOrder>
    ): Result<Boolean>
}
