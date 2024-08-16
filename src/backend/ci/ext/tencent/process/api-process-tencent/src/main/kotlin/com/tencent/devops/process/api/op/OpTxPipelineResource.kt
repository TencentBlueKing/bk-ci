package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE", description = "OP-流水线")
@Path("/op/pipelines/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTxPipelineResource {

    @Operation(summary = "修改流水线创建人")
    @PUT
    @Path("/{pipelineId}/creator")
    fun updatePipelineCreator(
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "新创建人", required = true)
        @QueryParam("creator")
        creator: String
    ): Result<Boolean>

    @Operation(summary = "修复流水线状态")
    @PUT
    @Path("/{pipelineId}/fixCheckOut")
    fun fixPipelineCheckOut(
        @Parameter(description = "stage数据失效时间", required = false)
        @QueryParam("stageTimeoutDays")
        stageTimeoutDays: Long?,
        @Parameter(description = "构建数据失效时间", required = false)
        @QueryParam("buildTimeoutDays")
        buildTimeoutDays: Long?
    ): Result<Int>
}
