package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ENVIRONMENT_THIRD_PARTY_AGENT", description = "内部第三方机资源")
@Path("/service/environment/tpa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAgentResource {
    @Operation(summary = "根据AgentId获取云桌面信息")
    @GET
    @Path("/getWorkspaceInfo")
    fun getWorkspaceInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "AgentHashId", required = true)
        @QueryParam("agentHashId")
        agentHashId: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "根据工作空间ID修改别名")
    @GET
    @Path("/updateDisplayNameByWorkspaceId")
    fun updateDisplayNameByWorkspaceId(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "workspaceId", required = true)
        @QueryParam("workspaceId")
        workspaceId: String,
        @Parameter(description = "displayName", required = true)
        @QueryParam("displayName")
        displayName: String
    )
}