package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_REPORT"], description = "构建上报")
@Path("/build/report")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildReportResource {

    @ApiOperation("插件上报度量信息")
    @POST
    @Path("/atom")
    fun atomReport(atomCode: String, data: String): Result<Boolean>
}
