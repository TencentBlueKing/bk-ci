package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_TEST"], description = "用户-测试接口")
@Path("/op/sessionCache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTestResource {

    @ApiOperation("清user-session缓存")
    @DELETE
    @Path("/clearByUerSessionId")
    fun clearUserSessionIdCache(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @ApiOperation("清session-page缓存")
    @DELETE
    @Path("/clearBySessionIdPage")
    fun clearSessionIdPageCache(
        @ApiParam("sessionId", required = true)
        @QueryParam("sessionId")
        sessionId: String
    ): Result<Boolean>

    @ApiOperation("page-sessionId缓存")
    @DELETE
    @Path("/clearByPageSessionId")
    fun clearPageSessionIdCache(
        @ApiParam("page", required = true)
        @QueryParam("page")
        page: String
    ): Result<Boolean>
}