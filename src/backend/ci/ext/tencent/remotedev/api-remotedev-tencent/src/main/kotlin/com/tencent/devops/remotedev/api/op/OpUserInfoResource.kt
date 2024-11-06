package com.tencent.devops.remotedev.api.op

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_USER_INFO", description = "OP_USER_INFO")
@Path("/op/userinfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpUserInfoResource {
    @Operation(summary = "用户权限中心续期审批回调")
    @POST
    @Path("/auth_check_callback")
    fun authCheckCallback(
        @QueryParam("id")
        id: Long
    )
}