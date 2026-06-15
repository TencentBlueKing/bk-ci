package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_CREATE", description = "创作环境管理")
@Path("/op/create")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpCreateResource {
    @Operation(summary = "删除节点")
    @POST
    @Path("/delete_create_node")
    fun deleteNodes(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    )
}