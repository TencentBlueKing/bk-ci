package com.tencent.devops.process.api.op

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE_CALLBACK"], description = "OP-流水线-回调接口禁用通知")
@Path("/op/pipeline/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineCallbackResource {

    @ApiOperation("发送接口禁用通知")
    @PUT
    @Path("/sendDisableNotify")
    fun sendDisableNotify(
        @ApiParam("蓝盾项目Id(项目英文名)", required = false)
        @QueryParam("projectId")
        projectId: String?
    )
}
