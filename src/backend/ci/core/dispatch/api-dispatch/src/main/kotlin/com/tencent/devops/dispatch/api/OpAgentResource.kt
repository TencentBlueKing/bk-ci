package com.tencent.devops.dispatch.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

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