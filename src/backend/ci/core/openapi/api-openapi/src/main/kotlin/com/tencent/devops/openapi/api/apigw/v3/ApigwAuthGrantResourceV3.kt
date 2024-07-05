package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_V3", description = "OPENAPI-权限相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwAuthGrantResourceV3 {

    @Operation(summary = "实例授权", tags = ["v3_app_permission_grant"])
    @POST
    @Path("/projects/{projectId}/instance/grant")
    fun grantInstancePermission(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        grantInstance: GrantInstanceDTO
    ): Result<Boolean>
}
