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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.PluginDetailsDisplayOrder
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OPENAPI_PROJECT_V4", description = "OPENAPI-项目资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwProjectResourceV4 {

    @POST
    @Path("/project_create")
    @Operation(summary = "创建项目", tags = ["v4_app_project_create", "v4_user_project_create"])
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
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}")
    @Operation(summary = "修改项目", tags = ["v4_user_project_edit", "v4_app_project_edit"])
    fun update(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "项目信息", required = true)
        projectUpdateInfo: ProjectUpdateInfo,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<Boolean>

    @GET
    @Path("/{projectId}")
    @Operation(summary = "获取项目信息", tags = ["v4_user_project_get", "v4_app_project_get"])
    fun get(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID英文名标识", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?
    ): Result<ProjectVO?>

    @GET
    @Path("/project_list")
    @Operation(summary = "查询所有项目", tags = ["v4_user_project_list", "v4_app_project_list"])
    fun list(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @Parameter(description = "产品Id,多个Id之间以,分隔", required = true)
        @QueryParam("productIds")
        productIds: String? = null
    ): Result<List<ProjectVO>>

    @GET
    @Path("/project_name_validation")
    @Operation(summary = "校验项目名称和项目英文名", tags = ["v4_app_project_name_validate", "v4_user_project_name_validate"])
    fun validate(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "校验的是项目名称或者项目英文名")
        @QueryParam("validateType")
        validateType: ProjectValidateType,
        @Parameter(description = "项目名称或者项目英文名")
        @QueryParam("name")
        name: String,
        @Parameter(description = "项目ID(项目英文名)")
        @QueryParam("english_name")
        projectId: String?
    ): Result<Boolean>

    @POST
    @Path("/{projectId}/project_user")
    @Operation(summary = "添加指定用户到指定项目用户组", tags = ["v4_app_project_create_users"])
    fun createProjectUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean?>

    @PUT
    @Path("/{projectId}/update_project_product")
    @Operation(
        summary = "更新项目关联产品",
        tags = ["v4_app_update_project_product", "v4_user_update_project_product"]
    )
    fun updateProjectProductId(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "产品名称", required = true)
        @QueryParam("productName")
        productName: String? = null,
        @Parameter(description = "产品Id", required = true)
        @QueryParam("productId")
        productId: Int? = null
    ): Result<Boolean>

    @GET
    @Path("/get_projects_by_product_id")
    @Operation(
        summary = "根据运营产品ID获取项目列表接口",
        tags = ["v4_app_get_projects_by_product_id", "v4_user_get_projects_by_product_id"]
    )
    fun getProjectListByProductId(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "产品ID", required = true)
        @QueryParam("productId")
        productId: Int
    ): Result<List<ProjectBaseInfo>>

    @PUT
    @Path("/{projectId}/update_plugin_details_display")
    @Operation(
        summary = "更新插件展示顺序",
        tags = ["v4_app_update_plugin_details_display"]
    )
    fun updatePluginDetailsDisplay(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "插件展示顺序", required = true)
        pluginDetailsDisplayOrder: List<PluginDetailsDisplayOrder>
    ): Result<Boolean>
}
