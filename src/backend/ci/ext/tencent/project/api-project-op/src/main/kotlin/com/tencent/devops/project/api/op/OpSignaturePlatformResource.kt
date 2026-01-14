package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.SignaturePlatformDetails
import com.tencent.devops.project.pojo.SignaturePlatformUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_SIGNATURE_PLATFORM", description = "OP-电子签相关")
@Path("/op/signature/platform")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpSignaturePlatformResource {
    @Operation(summary = "创建或更新保密签平台信息")
    @POST
    @Path("/")
    fun createOrUpdate(
        @Parameter(description = "平台信息", required = true)
        details: SignaturePlatformDetails
    ): Result<Boolean>

    @Operation(summary = "更新保密签平台展示内容")
    @PUT
    @Path("/{platform}/information")
    fun updateInformation(
        @Parameter(description = "平台标识", required = true)
        @PathParam("platform")
        platform: String,
        @Parameter(description = "更新内容", required = true)
        request: SignaturePlatformUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "删除保密签平台信息")
    @DELETE
    @Path("/{platform}/")
    fun delete(
        @Parameter(description = "平台id", required = true)
        @PathParam("platform")
        platform: String
    ): Result<Boolean>
}
