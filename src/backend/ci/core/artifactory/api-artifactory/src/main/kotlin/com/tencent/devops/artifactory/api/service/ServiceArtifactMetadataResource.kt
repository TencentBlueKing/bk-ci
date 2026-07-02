package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam

@Tag(name = "SERVICE_ARTIFACTORY_METADATA", description = "SERVICE-产出物元数据")
@Path("/service/artifactories/metadata")
@Produces("application/json")
interface ServiceArtifactMetadataResource {

    @Operation(summary = "查询产出物元数据")
    @GET
    @Path("/projects/{projectId}/artifacts/{artifactType}")
    fun getArtifactInfo(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID（可选）", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等", required = true)
        @PathParam("artifactType")
        artifactType: String,
        @Parameter(description = "产出物名称，如文件名、镜像名（可选）", required = false)
        @QueryParam("artifactName")
        artifactName: String?,
        @Parameter(description = "产出物版本，如镜像Tag、包版本（可选）", required = false)
        @QueryParam("artifactVersion")
        artifactVersion: String?
    ): Result<PipelineArtifactInfo?>
}
