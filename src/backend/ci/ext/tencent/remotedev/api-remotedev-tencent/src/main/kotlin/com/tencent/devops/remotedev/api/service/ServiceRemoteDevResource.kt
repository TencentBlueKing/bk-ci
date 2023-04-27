package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REMOTEDEV"], description = "remotedev service接口")
@Path("/service/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevResource {
    @ApiOperation("更新client对应环境的稳定版本")
    @PUT
    @Path("/updateClientVersion")
    fun updateClientVersion(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("client对应环境的环境信息", required = true)
        @QueryParam("env")
        env: String,
        @ApiParam("client对应环境的版本信息", required = true)
        @QueryParam("version")
        version: String
    ): Result<Boolean>
}
