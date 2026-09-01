package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "EXTERNAL_MARKET_EVENT", description = "外部-TAPD-事件触发")
@Path("/external/tapd/event")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalTapdEventResource {
    @Operation(summary = "TAPD webhook 事件推送")
    @POST
    @Path("/webhook")
    fun webhook(body: Map<String, Any>): Result<Boolean>
}
