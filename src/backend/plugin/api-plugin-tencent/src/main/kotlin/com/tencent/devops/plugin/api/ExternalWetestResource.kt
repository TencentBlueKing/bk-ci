package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_WETEST"], description = "服务-wetest相关")
@Path("/external/wetest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalWetestResource {

    @ApiOperation("wetest回调")
    @POST
    @Path("/task/callback")
    fun taskCallback(
        @ApiParam("测试任务ID", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("回调的详细数据", required = true)
        callback: Map<String, Any>
    ): Result<String>
}