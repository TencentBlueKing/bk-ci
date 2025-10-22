package com.tencent.devops.artifactory.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelDetail
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ARTIFACTORY_QUALITY_METADATA", description = "SERVICE-制品质量元数据")
@Path("/service/artifactories/quality/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArtifactQualityMetadataResource {

    @Operation(summary = "获取项目制品质量元数据标签")
    @GET
    @Path("/list/{projectId}")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<MetadataLabelDetail>>
}
