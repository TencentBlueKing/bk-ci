package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPEN_API_BUILD", description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTXPipelineResourceV4 {

    @Operation(summary = "导出流水线yaml,gitci", tags = ["v4_app_pipeline_stream_yaml", "v4_user_pipeline_stream_yaml"])
    @GET
    @Path("/stream/yaml")
    fun exportPipelineGitCI(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<PipelineExportV2YamlData>

    @Operation(
        summary = "查询指定用户(权限代持人)公开的可见流水线",
        tags = ["v4_app_pipeline_visibility_list", "v4_user_pipeline_visibility_list"]
    )
    @GET
    @Path("/visibility")
    fun listVisiblePipelines(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标用户ID(权限代持人)", required = true)
        @QueryParam("targetUserId")
        targetUserId: String,
        @Parameter(description = "流水线名称(模糊匹配)", required = false)
        @QueryParam("pipelineName")
        pipelineName: String? = null,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<SQLPage<SimplePipeline>>
}
