package com.tencent.devops.experience.api.open

import com.tencent.devops.common.api.auth.AUTH_HEADER_APP_VERSION
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_REAL_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_PLATFORM
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "OPEN_EXPERIENCE", description = "版本体验-公开接口")
@Path("/open/experiences/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenExperienceResource {
    @Operation(summary = "第三方登录")
    @Path("/outerLogin")
    @POST
    fun outerLogin(
        @Parameter(description = "平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @Parameter(description = "版本号", required = true)
        @HeaderParam(AUTH_HEADER_APP_VERSION)
        appVersion: String?,
        @HeaderParam(AUTH_HEADER_DEVOPS_REAL_IP)
        @Parameter(description = "用户IP", required = true)
        realIp: String,
        params: OuterLoginParam
    ): Result<String>

    @Operation(summary = "第三方鉴权")
    @Path("/outerAuth")
    @GET
    fun outerAuth(
        @Parameter(description = "凭证", required = true)
        @QueryParam("token")
        token: String
    ): Result<OuterProfileVO>

    @Operation(summary = "苹果应用商店跳转")
    @Path("/appstore/redirect")
    @GET
    fun appStoreRedirect(
        @Parameter(description = "公开体验ID", required = true)
        @QueryParam("id")
        @BkField(maxLength = 15)
        id: String,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        @BkField(maxLength = 15)
        userId: String
    ): Response
}
