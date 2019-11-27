package com.tencent.devops.prebuild.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["WEBIDE"], description = "WebIDE external 资源")
@Path("external/user/webide")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface WebIDEExResource {
    @GET
    @Path("/heartBeat/{ip}")
    @ApiOperation("ide心跳上报接口")
    fun heartBeat(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @ApiParam(value = "IDE实例的ip地址", required = true)
        ip: String
    ): Result<Boolean>
}
