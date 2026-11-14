package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlBranchActionReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDeployReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPullRequestReq
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
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PAC_PIPELINE_YAML_VERSION", description = "服务-PAC yaml 对应的流水线/模板版本")
@Path("/service/pipeline/yaml/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineYamlVersionResource {

    @Operation(summary = "创建 yaml 流水线/模板")
    @POST
    @Path("/projects/{projectId}")
    fun createYamlPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean,
        @Parameter(description = "部署请求", required = true)
        request: PipelineYamlDeployReq
    ): Result<DeployPipelineResult>

    @Operation(summary = "更新 yaml 流水线/模板")
    @PUT
    @Path("/projects/{projectId}/pipelines/{pipelineId}")
    fun updateYamlPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean,
        @Parameter(description = "部署请求", required = true)
        request: PipelineYamlDeployReq
    ): Result<DeployPipelineResult>

    @Operation(summary = "更新分支版本有效性")
    @PUT
    @Path("/projects/{projectId}/pipelines/{pipelineId}/branchAction")
    fun updateBranchAction(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean,
        @Parameter(description = "分支版本请求", required = true)
        request: PipelineYamlBranchActionReq
    ): Result<Boolean>

    @Operation(summary = "删除 yaml 流水线/模板")
    @DELETE
    @Path("/projects/{projectId}/pipelines/{pipelineId}")
    fun deletePipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean
    ): Result<Boolean>

    @Operation(summary = "获取 yaml 流水线/模板名称")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/name")
    fun getPipelineName(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean
    ): Result<String?>

    @Operation(summary = "判断是否已经存在正式版本")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/existsReleaseVersion")
    fun existsReleaseVersion(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean
    ): Result<Boolean>

    @Operation(summary = "合并请求完成")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/completePullRequest")
    fun completePullRequest(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID/模板ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否是模板", required = true)
        @QueryParam("isTemplate")
        isTemplate: Boolean,
        @Parameter(description = "合并请求", required = true)
        request: PipelineYamlPullRequestReq
    ): Result<Boolean>
}
