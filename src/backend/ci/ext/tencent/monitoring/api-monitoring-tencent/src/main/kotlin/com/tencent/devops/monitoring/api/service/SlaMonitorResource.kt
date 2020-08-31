package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.monitoring.pojo.SlaCodeccResponseData
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SLA"], description = "SLA监控")
@Path("/service/sla")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface SlaMonitorResource {
    @ApiOperation("SLA--CODECC统计")
    @POST
    @Path("/codecc/query")
    fun codeccQuery(
        @ApiParam(value = "事业群ID", required = true)
        @QueryParam("bgId")
        bgId: String,
        @ApiParam(value = "开始时间", required = true)
        @QueryParam("startTime")
        startTime: Long,
        @ApiParam(value = "结束时间", required = true)
        @QueryParam("endTime")
        endTime: Long
    ): Result<SlaCodeccResponseData>
}