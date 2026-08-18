package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_REMOTEDEV", description = "remotedev 纯内部获取信息不校验权限 service接口")
@Path("/service/remotedev_internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevInternalResource {
    @Operation(summary = "获取windows工作空间")
    @GET
    @Path("/workspace_detail")
    fun getWorkspaceDetail(
        @Parameter(description = "工作空间名", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<WorkspaceStartCloudDetail?>
}