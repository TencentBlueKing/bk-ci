package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CONTROL_POINT_MARKET"], description = "服务-质量红线-插件市场")
@Path("/service/controlPoint/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityControlPointMarketResource {

    @ApiOperation("")
    @Path("/setTestControlPoint")
    @POST
    fun setTestControlPoint(
        @QueryParam("userId")
        userId: String,
        controlPoint: QualityControlPoint
    ): Result<Int>

    @ApiOperation("刷新插件指标的指标")
    @Path("/refreshControlPoint")
    @PUT
    fun refreshControlPoint(
        @QueryParam("elementType")
        elementType: String
    ): Result<Int>

    @ApiOperation("删除插件指标的测试指标")
    @Path("/deleteTestControlPoint")
    @DELETE
    fun deleteTestControlPoint(
        @QueryParam("elementType")
        elementType: String
    ): Result<Int>
}