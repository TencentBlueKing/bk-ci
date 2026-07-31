package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.TeamInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_REMOTEDEV_RECORD", description = "remotedev开黑相关的 service接口")
@Path("/service/remotedev_team")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevTeamResource {
    @Operation(summary = "获取开黑信息，如果没有说明没开黑")
    @GET
    @Path("/get_workspace_team_info")
    fun getWorkspaceTeamInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @QueryParam("workspaceName")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "环境ID", required = true)
        @QueryParam("envId")
        envId: String?
    ): Result<TeamInfo?>

    @Operation(summary = "开启或关闭开黑功能")
    @POST
    @Path("/enable_workspace_team")
    fun enableWorkspaceTeam(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @QueryParam("workspaceName")
        projectId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "环境ID", required = true)
        @QueryParam("envId")
        envId: String?,
        @Parameter(description = "开启或关闭(true/false)", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<TeamInfo?>
}