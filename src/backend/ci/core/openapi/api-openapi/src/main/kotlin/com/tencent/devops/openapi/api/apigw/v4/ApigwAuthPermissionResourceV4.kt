package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.pojo.auth.ListUserResourcesByPermissionsReq
import com.tencent.devops.openapi.pojo.auth.UserResourcesByPermissionsVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_PERMISSION_V4", description = "OPENAPI-权限资源查询")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/permission/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwAuthPermissionResourceV4 {

    @POST
    @Path("/list_user_resources")
    @Operation(
        summary = "查询用户在项目下对指定操作拥有权限的资源实例列表",
        description = "按资源类型批量查询用户有权限的资源 Code。" +
            "适用于第三方系统按 view/edit/execute 等动作拉取可操作资源。" +
            "resourceType 与 actions 建议先通过权限元数据接口确认：" +
            "GET /v4/auth/metadata/list_resource_types、" +
            "GET /v4/auth/metadata/list_actions?resourceType={resourceType}。",
        tags = [
            "v4_app_list_user_resources_by_permissions",
            "v4_user_list_user_resources_by_permissions"
        ]
    )
    fun listUserResourcesByPermissions(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(
            description = "操作人用户ID。当请求体未传 userId 时，同时作为待查询用户",
            required = true
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(
            description = "查询条件。userId 可选，不传则查询请求头 X-DEVOPS-UID 对应用户。" +
                "返回 data.resources 按 actions 顺序排列，无权限时 resourceCodes 为空数组。",
            required = true,
            examples = [
                ExampleObject(
                    description = "查询某用户在项目下有查看/编辑权限的流水线",
                    value = """
                        {
                            "userId": "zhangsan",
                            "resourceType": "pipeline",
                            "actions": ["pipeline_view", "pipeline_edit"]
                        }"""
                )
            ]
        )
        request: ListUserResourcesByPermissionsReq
    ): Result<UserResourcesByPermissionsVO>
}
