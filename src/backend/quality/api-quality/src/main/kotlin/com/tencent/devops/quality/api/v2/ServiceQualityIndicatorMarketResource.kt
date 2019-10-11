package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
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

@Api(tags = ["SERVICE_INDICATOR_MARKET"], description = "服务-质量红线-插件市场")
@Path("/service/indicator/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityIndicatorMarketResource {

    @ApiOperation("注册插件指标")
    @Path("/setIndicator")
    @POST
    fun setTestIndicator(
        @QueryParam("userId")
        userId: String,
        @QueryParam("atomCode")
        atomCode: String,
        indicatorUpdateList: Collection<IndicatorUpdate>
    ): Result<Int>

    @ApiOperation("刷新插件指标的指标")
    @Path("/refreshMetadata")
    @PUT
    fun refreshIndicator(
        @QueryParam("elementType")
        elementType: String,
        metadataMap: Map<String /* dataId */, String /* id */>
    ): Result<Int>

    @ApiOperation("删除插件指标的测试指标")
    @Path("/deleteTestMetadata")
    @DELETE
    fun deleteTestIndicator(
        @QueryParam("elementType")
        elementType: String
    ): Result<Int>
}