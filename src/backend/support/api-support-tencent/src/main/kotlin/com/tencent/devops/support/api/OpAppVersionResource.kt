package com.tencent.devops.support.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.model.app.AppVersionRequest
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

/**
 * Created by Freyzheng on 2018/9/26.
 */

@Api(tags = ["OP_APP_VERSION"], description = "OP-APP-VERSION")
@Path("/op/app/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAppVersionResource {

    @ApiOperation("获取所有app版本日志")
    @GET
    @Path("/")
    fun getAllAppVersion(): Result<List<AppVersion>>

    @ApiOperation("获取app版本日志")
    @GET
    @Path("/{appVersionId}")
    fun getAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long
    ): Result<AppVersion?>

    @ApiOperation("新增app版本日志")
    @POST
    @Path("/")
    fun addAppVersion(
        @ApiParam(value = "APP版本", required = true)
        appVersionRequest: AppVersionRequest
    ): Result<Int>

    @ApiOperation("新增多个app版本日志")
    @POST
    @Path("/multi")
    fun addAppVersions(
        @ApiParam(value = "APP版本", required = true)
        appVersionRequests: List<AppVersionRequest>
    ): Result<Int>

    @ApiOperation("更新app版本日志")
    @PUT
    @Path("/{appVersionId}")
    fun updateAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long,
        @ApiParam(value = "APP版本", required = true)
        appVersionRequest: AppVersionRequest
    ): Result<Int>

    @ApiOperation("删除app版本日志")
    @DELETE
    @Path("/{appVersionId}")
    fun deleteAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long
    ): Result<Int>
}