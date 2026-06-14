package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_BATCH_TASK", description = "用户-流水线批量任务资源")
@Path("/user/pipeline/batch/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineBatchTaskResource {

    @Operation(summary = "查询流水线批量任务列表")
    @GET
    @Path("/{projectId}/tasks")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务类型", required = false)
        @QueryParam("type")
        type: PipelineBatchTaskType?,
        @Parameter(description = "任务状态", required = false)
        @QueryParam("status")
        status: PipelineBatchTaskStatus?,
        @Parameter(description = "创建人", required = false)
        @QueryParam("creator")
        creator: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        @DefaultValue("1")
        page: Int,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        @DefaultValue("20")
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTask>>

    @Operation(summary = "查询流水线批量任务数量")
    @GET
    @Path("/{projectId}/tasks/count")
    fun count(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务状态", required = false)
        @QueryParam("status")
        status: PipelineBatchTaskStatus?
    ): Result<Long>

    @Operation(summary = "创建流水线批量任务")
    @POST
    @Path("/{projectId}/tasks")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线批量任务创建请求", required = true)
        request: PipelineBatchTaskCreateRequest
    ): Result<String>

    @Operation(summary = "查询流水线批量任务详情")
    @GET
    @Path("/{projectId}/tasks/{taskId}")
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
    ): Result<PipelineBatchTask?>

    @Operation(summary = "查询流水线批量任务明细")
    @GET
    @Path("/{projectId}/tasks/{taskId}/details")
    fun listDetails(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "流水线创建人", required = false)
        @QueryParam("pipelineCreator")
        pipelineCreator: String?,
        @Parameter(description = "明细状态", required = false)
        @QueryParam("status")
        status: PipelineBatchTaskDetailStatus?,
        @Parameter(description = "是否开启PAC", required = false)
        @QueryParam("pac")
        pac: Boolean?,
        @Parameter(description = "是否是子流水线添加", required = false)
        @QueryParam("subPipeline")
        subPipeline: Boolean?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        @DefaultValue("1")
        page: Int,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        @DefaultValue("20")
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTaskDetailVo>>

    @Operation(summary = "查询流水线批量任务明细状态汇总")
    @GET
    @Path("/{projectId}/tasks/{taskId}/details/status/summary")
    fun detailStatusSummary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "任务类型", required = true)
        @QueryParam("taskType")
        taskType: PipelineBatchTaskType
    ): Result<List<PipelineBatchTaskDetailStatusSummary>>

    @Operation(summary = "排除流水线批量任务明细")
    @POST
    @Path("/{projectId}/tasks/{taskId}/pipelines/{pipelineId}/exclude")
    fun excludePipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "恢复流水线批量任务明细")
    @POST
    @Path("/{projectId}/tasks/{taskId}/pipelines/{pipelineId}/restore")
    fun restorePipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "恢复全部已排除的流水线批量任务明细")
    @POST
    @Path("/{projectId}/tasks/{taskId}/pipelines/restore")
    fun restoreAllPipelines(
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

    @Operation(summary = "执行流水线批量任务")
    @POST
    @Path("/{projectId}/tasks/{taskId}/execute")
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

    @Operation(summary = "删除流水线批量任务")
    @DELETE
    @Path("/{projectId}/tasks/{taskId}")
    fun delete(
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

    @Operation(summary = "重试失败流水线批量任务")
    @POST
    @Path("/{projectId}/tasks/{taskId}/retry")
    fun retry(
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

    @Operation(summary = "重试单个失败流水线批量任务明细")
    @POST
    @Path("/{projectId}/tasks/{taskId}/pipelines/{pipelineId}/retry")
    fun retryPipeline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>
}
