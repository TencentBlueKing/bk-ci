package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "APP_EXPERIENCE_SECURITY", description = "版本体验-安全")
@Path("/app/experiences/security")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceSecurityResource {
    @Operation(summary = "获取明文签名")
    @Path("/getClearSign")
    @GET
    fun getClearSign(
        @Parameter(description = "用户唯一标识", required = false)
        @QueryParam("openId")
        openId: String?,
        @Parameter(description = "用户昵称", required = false)
        @QueryParam("nickName")
        nickName: String?,
        @Parameter(description = "用户唯一标识", required = false)
        @QueryParam("openid")
        openid: String?,
        @Parameter(description = "用户昵称", required = false)
        @QueryParam("nickname")
        nickname: String?,
        @Parameter(description = "用户头像", required = true)
        @QueryParam("avatar")
        avatar: String
    ): Result<String>
}
