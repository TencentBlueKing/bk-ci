package com.tencent.devops.dispatch.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AGENT", description = "agent相关")
@Path("/op/agent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAgentResource {

    @Operation(summary = "修改灰度排队功能的项目或者流水线")
    @POST
    @Path("/update_gray_queue")
    fun updateGrayQueue(
        @QueryParam("projectId")
        projectId: String,
        @QueryParam("operate")
        operate: String,
        pipelineIds: Set<String>?
    )
}