package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_REAL_IP
import com.tencent.devops.dispatch.macos.pojo.PasswordInfo

@Tag(name = "BUILD_PASSWORD", description = "BUILD接口-密码资源")
@Path("build/macos/password")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildPasswordResource {
    @GET
    @Path("/")
    @Operation(summary = "获取vm列表")
    fun get(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "buildId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "realIp", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_REAL_IP)
        realIp: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        publicKey: String
    ): Result<PasswordInfo?>
}
