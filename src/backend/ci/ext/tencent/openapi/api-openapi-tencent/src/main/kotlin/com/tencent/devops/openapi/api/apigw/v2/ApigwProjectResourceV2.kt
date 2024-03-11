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
package com.tencent.devops.openapi.api.apigw.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_PROJECT_V2", description = "OPEN-API-项目资源V2")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v2/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwProjectResourceV2 {

    @POST
    @Path("/newProject")
    @Operation(summary = "创建项目", tags = ["v2_app_projects_newProject"])
    fun create(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo
    ): Result<String>

    @POST
    @Path("/newProject/setRouter")
    @Operation(summary = "创建项目", tags = ["v2_app_projects_setRouter"])
    fun createProjectSetRouter(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "项目对应的流量指向,一般为无权限校验的auto集群", required = true)
        @HeaderParam("routeTag")
        routerTag: String?,
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo
    ): Result<String>

    @GET
    @Path("/getProjectByOrganizationId")
    @Operation(summary = "根据组织架构查询所有项目", tags = ["v2_app_projects_getProjectByOrganizationId"])
    fun listProjectByOrganizationId(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @Parameter(description = "centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>?>

    @GET
    @Path("/getProjectByName")
    @Operation(summary = "根据名称查询项目信息,组织限制", tags = ["v2_app_projects_getProjectByName"])
    fun getProjectByOrganizationId(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "项目名称,精准匹配", required = true)
        @QueryParam("name")
        name: String,
        @Parameter(description = "名称类型: 中文名称、英文名称", required = true)
        @QueryParam("nameType")
        nameType: ProjectValidateType
    ): Result<ProjectVO?>

    @POST
    @Path("/{projectId}/createByUser")
    @Operation(summary = "添加指定用户到指定项目用户组", tags = ["v2_app_projects_createByUser"])
    fun createProjectUserByUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "执行用户Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUserId: String,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?>

    @POST
    @Path("/{projectId}/createUser")
    @Operation(summary = "添加指定用户到指定项目用户组", tags = ["v2_app_projects_createUser"])
    fun createProjectUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "执行用户Id", required = true)
        @QueryParam("createUserId")
        createUserId: String,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?>

    @POST
    @Path("/createUserByApp")
    @Operation(summary = "创建用户", tags = ["v2_app_projects_createUserByApp"])
    fun createProjectaUserByApp(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean?>

    @POST
    @Path("/permissions")
    @Operation(summary = "创建权限", tags = ["v2_app_projects_permissions"])
    fun createUserPipelinePermission(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "执行用户Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUserId: String?,
        @Parameter(description = "是否需要校验管理员", required = false)
        @HeaderParam("checkManager")
        checkManager: Boolean?,
        @Parameter(description = "添加信息", required = true)
        createInfo: PipelinePermissionInfo
    ): Result<Boolean?>

    @GET
    @Path("/{projectId}/roles")
    @Operation(summary = "获取roles", tags = ["v2_app_projects_roles"])
    fun getProjectRoles(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "项目code", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<BKAuthProjectRolesResources>?>
}
