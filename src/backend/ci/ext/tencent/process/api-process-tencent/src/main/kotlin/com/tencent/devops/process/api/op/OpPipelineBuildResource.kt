package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE", description = "OP-流水线")
@Path("/op/pipeline_builds/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineBuildResource {

    @Operation(summary = "修复流水线构建状态(将某几次的构建状态进行手动转换，如果从running -> finish or queue -> finish 需要同步修改summary)")
    @PUT
    @Path("/fix_build_status")
    fun fixPipelineBuildStatus(
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "转换前的状态码", required = true)
        @QueryParam("statusFrom")
        statusFrom: Int,
        @Parameter(description = "转换到的状态码", required = true)
        @QueryParam("statusTo")
        statusTo: Int,
        @Parameter(description = "构建号", required = false)
        @QueryParam("buildIds")
        buildIds: List<String>?
    ): Result<Int>

    @Operation(summary = "修复流水线构建状态(将某几次的构建状态进行手动转换，如果从running -> finish or queue -> finish 需要同步修改summary)")
    @PUT
    @Path("/fix_build_history")
    fun fixPipelineBuildHistory(
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "转换前的状态码", required = true)
        @QueryParam("statusFrom")
        statusFrom: Int,
        @Parameter(description = "转换到的状态码", required = true)
        @QueryParam("statusTo")
        statusTo: Int,
        @Parameter(description = "构建号", required = false)
        @QueryParam("buildIds")
        buildIds: List<String>?
    ): Result<Int>

    @Operation(summary = "修复流水线summary count计数信息)")
    @PUT
    @Path("/fix_summary_count")
    fun fixPipelineSummaryCount(
        @Parameter(description = "项目id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "需要变更的完成计数", required = false)
        @QueryParam("finishCount")
        finishCount: Int?,
        @Parameter(description = "需要变更的running计数", required = false)
        @QueryParam("runningCount")
        runningCount: Int?,
        @Parameter(description = "需要变更的排队计数", required = false)
        @QueryParam("queueCount")
        queueCount: Int?
    ): Result<Int>
}
