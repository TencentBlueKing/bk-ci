package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_INDICATOR_V2"], description = "质量红线-指标")
@Path("/service/indicators/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityIndicatorResource {

    @ApiOperation("获取单个指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/get")
    @GET
    fun get(
        @PathParam("projectId")
        projectId: String,
        @PathParam("indicatorId")
        indicatorId: String
    ): Result<QualityIndicator>
}