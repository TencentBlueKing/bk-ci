package com.tencent.devops.store.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ExtServiceInstallTrendReq
import com.tencent.devops.store.pojo.ExtServiceStatistic
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_SERVICE_STAT"], description = "服务扩展-统计")
@Path("/user/market/service/stat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceStatResource {

    @GET
    @ApiOperation("获取插件概述")
    @Path("/serviceCodes/{serviceCode}")
    fun getServiceStat(
        @ApiParam("扩展服务编码")
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<ExtServiceStatistic>

    @GET
    @ApiOperation("安装量趋势")
    @Path("/serviceCodes/{serviceCode}")
    fun getInstallTrend(
        @ApiParam("扩展服务编码")
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("天数")
        @QueryParam("days")
        days: Int
    ): Result<List<ExtServiceInstallTrendReq>>
}