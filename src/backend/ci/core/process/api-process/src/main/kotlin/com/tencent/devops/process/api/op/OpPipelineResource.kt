package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE", description = "OP-流水线资源")
@Path("/op/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineResource {

    @Operation(summary = "跨项目复制流水线")
    @POST
    @Path("/projects/{sourceProjectId}/copyAcrossProject")
    fun copyAcrossProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "源项目ID", required = true)
        @PathParam("sourceProjectId")
        sourceProjectId: String,
        @Parameter(description = "目标项目ID", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "源流水线ID，为空则复制源项目下全部流水线", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>

    @Operation(summary = "修复约束模板实例缺失的流水线设置(临时)")
    @POST
    @Path("/projects/{sourceProjectId}/fixInstanceSetting")
    fun fixInstanceSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "源项目ID", required = true)
        @PathParam("sourceProjectId")
        sourceProjectId: String,
        @Parameter(description = "目标项目ID", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>
}
