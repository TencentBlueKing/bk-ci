package com.tencent.devops.support.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.pojo.AppVersion
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam

/**
 * Created by Freyzheng on 2018/9/26.
 */

@Api(tags = ["APP_APP_VERSION"], description = "APP-APP-VERSION")
@Path("/app/app/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppAppVersionResource {

    @ApiOperation("获取最新的app版本号")
    @GET
    @Path("/last")
    fun getLastAppVersion(
        @ApiParam(value = "渠道类型（1:\"安卓\", 2:\"IOS\", 3:\"WEB\"）", required = true)
        @QueryParam(value = "channelType")
        channelType: Byte
    ): Result<AppVersion?>

    @ApiOperation("获取所有的app版本号")
    @GET
    @Path("/")
    fun getAllAppVersion(
        @ApiParam(value = "渠道类型（1:\"安卓\", 2:\"IOS\", 3:\"WEB\"）", required = true)
        @QueryParam(value = "channelType")
        channelType: Byte
    ): Result<List<AppVersion>>
}