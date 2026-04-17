package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
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
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/resource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthResourceV4 {
    @GET
    @Path("/list_resource_types")
    @Operation(
        summary = "获取资源类型列表",
        tags = [
            "v4_app_list_resource_types",
            "v4_user_list_resource_types"
        ]
    )
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].resourceType}"], responseDataCacheFlag = true)
    fun listResourceTypes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("/list_actions")
    @Operation(
        summary = "获取操作类型列表",
        tags = [
            "v4_app_list_actions",
            "v4_user_list_actions"
        ]
    )
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].action}"], responseDataCacheFlag = true)
    fun listActions(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ActionInfoVo>>

    @GET
    @Path("/get_resource_by_name")
    @Operation(
        summary = "根据资源名称查询资源",
        tags = [
            "v4_app_get_resource_by_name",
            "v4_user_get_resource_by_name"
        ]
    )
    fun getResourceByName(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目Id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源名称", required = true)
        @QueryParam("resourceName")
        resourceName: String
    ): Result<AuthResourceInfo?>

    @GET
    @Path("/get_resource_by_code")
    @Operation(
        summary = "根据资源code查询资源",
        tags = [
            "v4_app_get_resource_by_code",
            "v4_user_get_resource_by_code"
        ]
    )
    fun getResourceByCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目Id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code", required = true)
        @QueryParam("resourceCode")
        resourceCode: String
    ): Result<AuthResourceInfo?>
}
