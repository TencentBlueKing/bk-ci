package com.tencent.devops.experience.api.open

import com.tencent.devops.common.api.auth.AUTH_HEADER_APP_VERSION
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_REAL_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_PLATFORM
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
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
import javax.ws.rs.core.Response

@Api(tags = ["OPEN_EXPERIENCE"], description = "版本体验-公开接口")
@Path("/open/experiences/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenExperienceResource {
    @ApiOperation("第三方登录")
    @Path("/outerLogin")
    @POST
    fun outerLogin(
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("版本号", required = true)
        @HeaderParam(AUTH_HEADER_APP_VERSION)
        appVersion: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_REAL_IP)
        @ApiParam("用户IP", required = true)
        realIp: String,
        params: OuterLoginParam
    ): Result<String>

    @ApiOperation("第三方鉴权")
    @Path("/outerAuth")
    @GET
    fun outerAuth(
        @ApiParam("凭证", required = true)
        @QueryParam("token")
        token: String
    ): Result<OuterProfileVO>

    @ApiOperation("苹果应用商店跳转")
    @Path("/appstore/redirect")
    @GET
    fun appStoreRedirect(
        @ApiParam("公开体验ID", required = true)
        @QueryParam("id")
        @BkField(maxLength = 15)
        id: String,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        @BkField(maxLength = 15)
        userId: String
    ): Response
}
