package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Created by rdeng on 2017/9/4.
 */
@Api(tags = ["OP_PIPELINE"], description = "PIPELINE 管理")
@Path("/op/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineResource {

    @ApiOperation("设置构建机对应的虚拟机")
    @POST
    @Path("/{pipelineId}/setVMs")
    fun setVMs(
        @ApiParam(value = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmNames", required = true)
        @QueryParam("vmNames")
        vmNames: String,
        @ApiParam(value = "vmSeqId", required = false)
        @QueryParam("vmSeqId")
        vmSeqId: Int?
    ): Result<Boolean>

    @ApiOperation("获取构建机对应的虚拟机")
    @GET
    @Path("/{pipelineId}/getVMs")
    fun getVMs(
        @ApiParam(value = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = false)
        @QueryParam("vmSeqId")
        vmSeqId: Int?
    ): Result<String>
}
