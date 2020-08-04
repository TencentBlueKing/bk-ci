package com.tencent.devops.auth.api.callback

import com.tencent.devops.auth.pojo.BkResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_CALLBACK"], description = "权限-回调接口")
@Path("/service/auth/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AuthCallBackResource {

    @GET
    @Path("/healthz1")
    @ApiOperation("权限系统心跳接口")
    fun healthz(): BkResult<Boolean>
}