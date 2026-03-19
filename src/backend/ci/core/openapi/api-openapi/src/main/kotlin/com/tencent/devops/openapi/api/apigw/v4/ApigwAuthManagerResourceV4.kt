package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.pojo.ProjectManagerRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_MANAGER_V4", description = "OPENAPI-项目管理员")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/manager")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthManagerResourceV4 {

    @POST
    @Path("/getProjectManagers")
    @Operation(
        summary = "获取项目管理员列表",
        tags = ["v4_app_get_project_managers"]
    )
    fun getProjectManagers(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "请求体", required = true)
        request: ProjectManagerRequest
    ): Result<List<String>>
}
