package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "BUILD_REPORT", description = "构建上报")
@Path("/build/atom/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAtomMetricsResource {

    @Operation(summary = "插件上报度量信息")
    @POST
    @Path("/report/{atomCode}")
    fun reportAtomMetrics(
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "上报度量的数据", required = true)
        data: String
    ): Result<Boolean>
}
