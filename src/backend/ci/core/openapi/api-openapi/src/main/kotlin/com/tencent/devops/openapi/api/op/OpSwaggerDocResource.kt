package com.tencent.devops.openapi.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.SwaggerDocResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

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
