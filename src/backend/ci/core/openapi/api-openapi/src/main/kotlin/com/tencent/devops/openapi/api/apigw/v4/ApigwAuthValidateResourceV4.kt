package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_V4", description = "OPENAPI-权限相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/validate/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthValidateResourceV4 {
    @GET
    @Path("/check_project_users")
    @Operation(
        summary = "判断是否某个项目中某个组角色的成员",
        tags = ["v4_app_permission_project_check", "v4_user_permission_project_check"]
    )
    fun isProjectUser(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目Code", required = true)
        projectId: String,
        @QueryParam("group")
        @Parameter(description = "用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<Boolean>

    @GET
    @Path("/check_user_in_project_level_group/{userId}")
    @Operation(
        summary = "检查用户是否在项目级别的用户组中",
        tags = ["v4_app_check_user_in_project_level_group"]
    )
    fun checkUserInProjectLevelGroup(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/permission/validate")
    @Operation(
        summary = "校验用户是否有具体资源实例的操作权限",
        tags = ["v4_app_validate_user_resource_permission"]
    )
    fun validateUserResourcePermission(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @QueryParam("action")
        @Parameter(description = "action类型", required = true)
        action: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = true)
        resourceCode: String
    ): Result<Boolean>
}
