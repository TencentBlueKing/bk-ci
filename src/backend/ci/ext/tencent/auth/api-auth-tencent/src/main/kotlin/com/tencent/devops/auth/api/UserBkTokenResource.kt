package com.tencent.devops.auth.api

import com.tencent.devops.auth.pojo.BkAccessTokenInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
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

@Tag(name = "AUTH_BK_TOKEN", description = "用户态-蓝鲸平台access_token")
@Path("/user/bkToken")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserBkTokenResource {

    @Operation(summary = "获取蓝鲸平台access_token")
    @GET
    @Path("/get")
    fun getAccessToken(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝鲸登录态", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String
    ): Result<BkAccessTokenInfo>
}
