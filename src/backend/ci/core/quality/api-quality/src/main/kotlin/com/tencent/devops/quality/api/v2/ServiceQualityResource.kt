package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_METADATA", description = "质量红线-外部")
@Path("/service/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityResource {
    @Operation(summary = "元数据回调")
    @Path("/project/{projectId}/pipeline/{pipelineId}/build/{buildId}/metadata/callback")
    @POST
    fun metadataCallback(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        callback: MetadataCallback
    ): Result<String>
}
