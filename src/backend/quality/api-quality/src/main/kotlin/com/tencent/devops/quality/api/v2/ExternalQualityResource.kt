package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_METADATA"], description = "质量红线-外部")
@Path("/external/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalQualityResource {
    @ApiOperation("元数据回调")
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