package com.tencent.devops.artifactory.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelDetail
import com.tencent.devops.common.archive.pojo.MetadataLabelDetailUpdate
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_ARTIFACTORY_QUALITY_METADATA", description = "USER-制品质量元数据")
@Path("/user/artifactories/quality/metadata/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserArtifactQualityMetadataResource {
    @Operation(summary = "获取项目制品质量元数据标签列表")
    @GET
    @Path("/")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<MetadataLabelDetail>>

    @Operation(summary = "获取流水线级别制品质量元数据")
    @GET
    @Path("/pipeline/{pipelineId}")
    fun listByPipeline(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
    ): Result<List<MetadataLabelDetail>>

    @Operation(summary = "获取项目制品质量元数据标签")
    @GET
    @Path("/{labelKey}")
    fun get(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签KEY", required = true)
        @PathParam("labelKey")
        labelKey: String
    ): Result<MetadataLabelDetail>

    @Operation(summary = "删除项目制品质量元数据标签")
    @DELETE
    @Path("/{labelKey}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签KEY", required = true)
        @PathParam("labelKey")
        labelKey: String
    ): Result<Boolean>

    @Operation(summary = "更新项目制品质量元数据标签")
    @PUT
    @Path("/{labelKey}")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签KEY", required = true)
        @PathParam("labelKey")
        labelKey: String,
        @Parameter(description = "请求体", required = true)
        metadataLabelUpdate: MetadataLabelDetailUpdate
    ): Result<Boolean>

    @Operation(summary = "创建项目制品质量元数据标签")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "请求体", required = true)
        metadataLabel: MetadataLabelDetail
    ): Result<Boolean>
}
