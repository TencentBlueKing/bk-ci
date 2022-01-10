package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_REPORT"], description = "构建上报")
@Path("/build/atom/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAtomMetricsResource {

    @ApiOperation("插件上报度量信息")
    @POST
    @Path("/report/{atomCode}")
    fun reportAtomMetrics(
        @ApiParam(value = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam(value = "上报度量的数据", required = true)
        data: String
    ): Result<Boolean>
}
