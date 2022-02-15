package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE"], description = "OP-流水线")
@Path("/op/pipelines/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTxPipelineResource {

    @ApiOperation("修改流水线创建人")
    @PUT
    @Path("/{pipelineId}/creator")
    fun updatePipelineCreator(
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("新创建人", required = true)
        @QueryParam("creator")
        creator: String
    ): Result<Boolean>

    @ApiOperation("修复流水线状态")
    @PUT
    @Path("/{pipelineId}/fixCheckOut")
    fun fixPipelineCheckOut(
        @ApiParam("stage数据失效时间", required = false)
        @QueryParam("stageTimeoutDays")
        stageTimeoutDays: Long?,
        @ApiParam("构建数据失效时间", required = false)
        @QueryParam("buildTimeoutDays")
        buildTimeoutDays: Long?
    ): Result<Int>
}
