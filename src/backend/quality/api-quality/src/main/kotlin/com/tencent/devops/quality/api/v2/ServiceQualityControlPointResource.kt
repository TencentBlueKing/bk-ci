package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CONTROL_POINT_V2"], description = "质量红线-拦截点v2")
@Path("/service/controlPoints/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityControlPointResource {

    @ApiOperation("获取控制点信息")
    @Path("/set")
    @POST
    fun set(
        @QueryParam("userId")
        userId: String,
        controlPoint: QualityControlPoint
    ): Result<Int>

    @ApiOperation("清除控制点信息")
    @Path("/cleanTestProject")
    @PUT
    fun cleanTestProject(
        @QueryParam("userId")
        controlPointType: String
    ): Result<Int>
}