package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.VersionLog
import com.tencent.devops.store.pojo.vo.VersionLogVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXT_SERVICE_VERSION_LOG"], description = "扩展服务-版本日志")
@Path("/user/market/service/version/logs/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceVersionLogResource {

    @Path("serviceIds/{serviceId}/list")
    @GET
    @ApiOperation("获取版本日志列表")
    fun getVersionLogList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<VersionLogVO?>

    @Path("/ids/{logId}")
    @GET
    @ApiOperation("获取单条版本日志")
    fun getVersionLog(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("logId", required = true)
        @PathParam("logId")
        logId: String
    ): Result<VersionLog>
}