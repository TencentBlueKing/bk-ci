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

@Api(tags = ["OP_PIPELINE_CALLBACK"], description = "OP-流水线-回调接口禁用通知")
@Path("/op/pipeline/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineCallbackResource {

    @ApiOperation("根据ID恢复回调接口")
    @PUT
    @Path("/{projectId}/enableCallback/byId")
    fun enableCallbackByIds(
        @ApiParam("蓝盾项目Id(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("回调信息ID", required = true)
        @QueryParam("callbackIds")
        callbackIds: String
    ): Result<Boolean>

    @ApiOperation("根据Url恢复回调接口")
    @PUT
    @Path("/{projectId}/enableCallback/byUrl")
    fun enableCallbackByUrl(
        @ApiParam("蓝盾项目Id(项目英文名)", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("回调url", required = true)
        @QueryParam("url")
        url: String
    ): Result<Boolean>
}
