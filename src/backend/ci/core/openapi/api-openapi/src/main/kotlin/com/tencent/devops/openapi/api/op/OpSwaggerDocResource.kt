package com.tencent.devops.openapi.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.SwaggerDocResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_APP_MANAGER_INFO", description = "OP-AppCode管理员")
@Path("/op/swaggerDoc/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpSwaggerDocResource {
    @Operation(summary = "文档输出")
    @GET
    @Path("/init")
    fun docInit(
        @Parameter(description = "checkMetaData", required = false)
        @QueryParam("checkMetaData")
        @DefaultValue("false")
        checkMetaData: Boolean,
        @Parameter(description = "checkMDData", required = false)
        @QueryParam("checkMDData")
        @DefaultValue("true")
        checkMDData: Boolean
    ): Result<Map<String, SwaggerDocResponse>>
}
