package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceBasicInfoGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceDetailGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_COPY", description = "用户-流水线复制资源")
@Path("/user/pipeline/copy/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineCopyTaskResource {

    @Operation(summary = "创建流水线复制任务")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线复制任务创建请求", required = true)
        request: PipelineCopyTaskCreateRequest
    ): Result<String>

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
    ): Result<PipelineCopyTaskInfo?>

    @Operation(summary = "获取流水线复制任务状态")
    @GET
    @Path("/{taskId}/status")
    fun taskStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<PipelineCopyTaskStatus>

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

    @Operation(summary = "列举流水线复制资源详情")
    @GET
    @Path("/{taskId}/resources/details")
    fun listResourceDetails(
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
        resourceName: String?
    ): Result<List<PipelineCopyResourceDetailGroup>>

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
        @Parameter(description = "流水线复制资源信息", required = true)
        resources: List<PipelineCopyResourceDetail>
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
        taskId: String,
        @Parameter(description = "流水线复制资源信息", required = true)
        resources: List<PipelineCopyResourceDetail>
    ): Result<Boolean>

    @Operation(summary = "获取流水线复制执行状态")
    @GET
    @Path("/{taskId}/execute/status")
    fun executeStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<PipelineCopyTaskExecuteStatus>

    @Operation(summary = "获取流水线复制资源基础信息")
    @GET
    @Path("/{taskId}/resources/basic")
    fun listResourceBasicInfo(
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
        @Parameter(description = "资源是否需要补齐", required = false)
        @QueryParam("needCompletion")
        needCompletion: Boolean? = null,
        @Parameter(description = "资源是否需要处理", required = false)
        @QueryParam("needTransfer")
        needTransfer: Boolean? = null,
        @Parameter(description = "是否自动完成", required = false)
        @QueryParam("autoFinish")
        autoFinish: Boolean? = null
    ): Result<List<PipelineCopyResourceBasicInfoGroup>>

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
