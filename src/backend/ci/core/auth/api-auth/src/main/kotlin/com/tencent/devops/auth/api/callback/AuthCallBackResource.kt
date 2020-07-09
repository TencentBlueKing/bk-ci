package com.tencent.devops.auth.api.callback

import com.tencent.devops.auth.pojo.BkResult
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_CALLBACK"], description = "权限-回调接口")
@Path("/service/auth/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AuthCallBackResource {

    @GET
    @Path("/healthz")
    @ApiOperation("权限系统心跳接口")
    fun healthz(): BkResult<Boolean>
}