package com.tencent.devops.experience.api.open

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_REAL_IP
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_EXPERIENCE"], description = "版本体验-公开接口")
@Path("/open/experiences/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenExperienceResource {
    @ApiOperation("第三方登录")
    @Path("/outerLogin")
    @POST
    fun outerLogin(
        @HeaderParam(AUTH_HEADER_DEVOPS_REAL_IP)
        @ApiParam("用户IP", required = true)
        realIp: String,
        params: OuterLoginParam
    ): Result<String>

    @ApiOperation("第三方鉴权")
    @Path("/outerAuth")
    @GET
    fun outerAuth(
        @QueryParam("token")
        token: String
    ): Result<OuterProfileVO>
}
