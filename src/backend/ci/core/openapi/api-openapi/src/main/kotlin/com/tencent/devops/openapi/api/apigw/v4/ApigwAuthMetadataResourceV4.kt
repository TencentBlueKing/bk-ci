package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
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

@Tag(name = "OPENAPI_AUTH_METADATA_V4", description = "OPENAPI-权限元数据")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthMetadataResourceV4 {

    @GET
    @Path("/list_resource_types")
    @Operation(
        summary = "获取资源类型列表",
        tags = ["v4_app_list_auth_resource_types", "v4_user_list_auth_resource_types"]
    )
    fun listResourceTypes(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("/list_actions")
    @Operation(
        summary = "获取资源类型对应的操作列表",
        tags = ["v4_app_list_auth_actions", "v4_user_list_auth_actions"]
    )
    fun listActions(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "资源类型(与权限模型中的资源类型标识一致)", required = true)
        @QueryParam("resourceType")
        resourceType: String
    ): Result<List<ActionInfoVo>>

    @GET
    @Path("/projects/{projectId}/search_resource")
    @Operation(
        summary = "根据资源名称或 Code 搜索资源",
        tags = ["v4_app_search_auth_resource", "v4_user_search_auth_resource"]
    )
    fun searchResource(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型(与权限模型中的资源类型标识一致)", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "搜索关键词(匹配资源名称或资源Code)", required = true)
        @QueryParam("keyword")
        keyword: String
    ): Result<List<AuthResourceInfo>>

    @GET
    @Path("/projects/{projectId}/get_resource_by_name")
    @Operation(
        summary = "根据名称查询资源",
        tags = ["v4_app_get_auth_resource_by_name", "v4_user_get_auth_resource_by_name"]
    )
    fun getResourceByName(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型(与权限模型中的资源类型标识一致)", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源名称(精确或业务侧约定的名称)", required = true)
        @QueryParam("resourceName")
        resourceName: String
    ): Result<AuthResourceInfo?>

    @GET
    @Path("/projects/{projectId}/get_resource_by_code")
    @Operation(
        summary = "根据 Code 查询资源",
        tags = ["v4_app_get_auth_resource_by_code", "v4_user_get_auth_resource_by_code"]
    )
    fun getResourceByCode(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型(与权限模型中的资源类型标识一致)", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code(业务侧唯一标识)", required = true)
        @QueryParam("resourceCode")
        resourceCode: String
    ): Result<AuthResourceInfo?>
}
