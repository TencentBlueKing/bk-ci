package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_REMOTEDEV_AI", description = "用户-AI")
@Path("/user/coffee_ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCoffeeAIResource {

    @Operation(summary = "获取WebSocket连接令牌")
    @GET
    @Path("/token")
    fun getWebSocketToken(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<String>
}
