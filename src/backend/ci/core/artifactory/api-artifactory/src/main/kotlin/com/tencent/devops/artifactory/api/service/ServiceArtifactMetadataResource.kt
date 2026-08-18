package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
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

    @Operation(summary = "分页查询制品元数据列表")
    @GET
    @Path("/projects/{projectId}/artifacts/list")
    fun listArtifactInfo(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID（可选）", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等（不传返回全部类型）", required = false)
        @QueryParam("artifactType")
        artifactType: String?,
        @Parameter(description = "产出物名称，如文件名、镜像名", required = false)
        @QueryParam("artifactName")
        artifactName: String?,
        @Parameter(description = "产出物版本，如镜像Tag、包版本", required = false)
        @QueryParam("artifactVersion")
        artifactVersion: String?,
        @Parameter(description = "执行次数（可选），不传则匹配所有执行次数；传 buildId 时不传则取当次构建最新", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "构建ID（可选）", required = false)
        @QueryParam("buildId")
        buildId: String?,
        @Parameter(description = "第几页，默认1", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数，默认20", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineArtifactInfo>>
}
