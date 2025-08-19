package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_V3", description = "OPENAPI-权限相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/auth/validate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v3")
interface ApigwAuthValidateResourceV3 {
    @GET
    @Path("/projects/{projectId}/isProjectUsers")
    @Operation(
        summary = "判断是否某个项目中某个组角色的成员",
        tags = ["v3_app_permission_project_check", "v3_user_permission_project_check"]
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
}
