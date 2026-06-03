package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteProgress
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSaveResourceRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_COPY_TASK", description = "用户-流水线复制资源")
@Path("/user/pipeline/copy/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineCopyTaskResource {

    @Operation(summary = "获取流水线复制任务")
    @GET
    @Path("/{taskId}")
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<PipelineCopyTask?>

    @Operation(summary = "保存流水线复制配置草稿")
    @POST
    @Path("/{taskId}/config/draft")
    fun saveConfigDraft(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线复制配置草稿请求", required = true)
        @Valid
        request: PipelineCopyTaskConfigRequest
    ): Result<Boolean>

    @Operation(summary = "分析流水线复制资源依赖")
    @POST
    @Path("/{taskId}/resources/depend/analysis")
    fun analyzeResourceDepend(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线复制资源依赖分析请求", required = true)
        request: PipelineCopyTaskConfigRequest
    ): Result<Boolean>

    @Operation(summary = "列举流水线复制资源")
    @GET
    @Path("/{taskId}/resources")
    fun listResource(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "资源类型", required = false)
        @QueryParam("resourceType")
        resourceType: PipelineDependentResourceType?,
        @Parameter(description = "资源名", required = false)
        @QueryParam("resourceName")
        resourceName: String?,
        @Parameter(description = "资源复制动作", required = false)
        @QueryParam("copyAction")
        copyAction: PipelineCopyAction? = null
    ): Result<List<PipelineCopyResourceGroup>>

    @Operation(summary = "查询流水线复制资源关联的流水线")
    @GET
    @Path("/{taskId}/resources/{resourceType}/{resourceId}/pipelines")
    fun listResourcePipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: PipelineDependentResourceType,
        @Parameter(description = "资源ID", required = true)
        @PathParam("resourceId")
        resourceId: String,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?
    ): Result<List<PipelineCopyPipelineInfo>>

    @Operation(summary = "保存流水线复制资源草稿")
    @POST
    @Path("/{taskId}/resources/draft")
    fun saveResourceDraft(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线复制资源保存请求", required = true)
        request: PipelineCopyTaskSaveResourceRequest
    ): Result<Boolean>

    @Operation(summary = "准备执行流水线复制")
    @POST
    @Path("/{taskId}/execute/prepare")
    fun prepareExecute(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线复制资源保存请求", required = true)
        request: PipelineCopyTaskSaveResourceRequest
    ): Result<Boolean>

    @Operation(summary = "执行流水线复制")
    @POST
    @Path("/{taskId}/execute")
    fun execute(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<Boolean>

    @Operation(summary = "获取流水线复制执行汇总")
    @GET
    @Path("/{taskId}/execute/summary")
    fun executeSummary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<PipelineCopyTaskExecuteSummary>

    @Operation(summary = "获取流水线复制执行进度")
    @GET
    @Path("/{taskId}/execute/progress")
    fun executeProgress(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<PipelineCopyTaskExecuteProgress>

    @Operation(summary = "确认流水线复制资源")
    @POST
    @Path("/{taskId}/resources/{resourceType}/{resourceId}/confirm")
    fun confirmResource(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: PipelineDependentResourceType,
        @Parameter(description = "资源ID", required = true)
        @PathParam("resourceId")
        resourceId: String,
        @Parameter(description = "是否确认", required = true)
        @QueryParam("confirmed")
        confirmed: Boolean
    ): Result<Boolean>
}
