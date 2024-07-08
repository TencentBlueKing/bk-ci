package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.gitproxy.CallbackLinktgitData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_CODE_PROY", description = "OP_CODE_PROY")
@Path("/op/codeproxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpCodeProxyResource {

    @Operation(summary = "回调关联工蜂库")
    @POST
    @Path("/callback/tgitlink")
    fun tgitlink(
        data: CallbackLinktgitData
    ): Result<Map<Long, Boolean>>
}
