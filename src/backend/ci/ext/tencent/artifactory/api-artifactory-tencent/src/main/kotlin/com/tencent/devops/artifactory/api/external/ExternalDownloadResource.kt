package com.tencent.devops.artifactory.api.external

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "EXTERNAL_ARTIFACTORY", description = "版本仓库-仓库资源")
@Path("/external/artifactories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList", "TooManyFunctions")
interface ExternalDownloadResource {
    @Operation(summary = "获取构建HAP文件的json5下载文件")
    @Path("/{projectId}/{artifactoryType}/{token}/hapJson5.json5")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @GET
    fun getHapJson5(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "Token", required = true)
        @PathParam("token")
        token: String
    ): String
}