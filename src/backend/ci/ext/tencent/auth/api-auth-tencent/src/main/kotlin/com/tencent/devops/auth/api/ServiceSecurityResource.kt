package com.tencent.devops.auth.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_SECURITY", description = "安全相关")
@Path("/service/security")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSecurityResource {
    @Operation(summary = "校验用户是否有访问项目权限", tags = ["v4_app_security_verify_project_user"])
    @GET
    @Path("/{projectId}/verifyProjectUser")
    fun verifyProjectUser(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据", required = true)
        @QueryParam("credentialKey")
        credentialKey: String
    ): Result<Boolean?>

    @Operation(summary = "获取水印信息", tags = ["v4_app_security_water_mark_get"])
    @GET
    @Path("/{projectId}/getUserWaterMark")
    fun getUserWaterMark(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据", required = true)
        @QueryParam("credentialKey")
        credentialKey: String
    ): Result<String?>
}
