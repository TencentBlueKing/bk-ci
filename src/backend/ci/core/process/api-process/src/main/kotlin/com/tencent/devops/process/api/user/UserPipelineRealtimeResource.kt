package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_REALTIME", description = "流水线实时状态只读查询")
@Path("/user/pipelines/realtime")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineRealtimeResource {
    @POST
    @Path("/{projectId}/status")
    @Operation(summary = "批量查询可见流水线的完整运行状态")
    fun status(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        pipelineIds: Set<String>
    ): Result<Map<String, PipelineStatus>>
}
