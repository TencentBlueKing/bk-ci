package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.FixPipelineSubPipelineProjectRequest
import com.tencent.devops.process.pojo.pipeline.FixTemplateSubPipelineProjectRequest
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

@Tag(name = "OP_PIPELINE_COPY", description = "OP-流水线复制")
@Path("/op/pipelines/copy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineCopyResource {

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
        @Parameter(description = "源流水线ID", required = true)
        @QueryParam("sourcePipelineId")
        sourcePipelineId: String,
        @Parameter(description = "目标流水线ID，为空则与源流水线ID相同", required = false)
        @QueryParam("targetPipelineId")
        targetPipelineId: String?
    ): Result<Boolean>

    @Operation(summary = "跨项目复制流水线标签")
    @POST
    @Path("/labels/projects/{sourceProjectId}/copyAcrossProject")
    fun copyLabelsAcrossProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "源项目ID", required = true)
        @PathParam("sourceProjectId")
        sourceProjectId: String,
        @Parameter(description = "目标项目ID", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "源标签ID，为空则复制源项目下全部标签", required = false)
        @QueryParam("labelId")
        labelId: String?
    ): Result<Boolean>

    @Operation(summary = "修复流水线子流水线插件项目ID(临时)")
    @POST
    @Path("/projects/{projectId}/fixPipelineSubPipelineProject")
    fun fixPipelineSubPipelineProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "修复流水线子流水线插件项目ID请求体", required = true)
        request: FixPipelineSubPipelineProjectRequest
    ): Result<Boolean>

    @Operation(summary = "修复模板子流水线插件项目ID(临时)")
    @POST
    @Path("/projects/{projectId}/fixTemplateSubPipelineProject")
    fun fixTemplateSubPipelineProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "修复模板子流水线插件项目ID请求体", required = true)
        request: FixTemplateSubPipelineProjectRequest
    ): Result<Boolean>

    @Operation(summary = "跨项目复制流水线组")
    @POST
    @Path("/views/projects/{sourceProjectId}/copyAcrossProject")
    fun copyViewsAcrossProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "源项目ID", required = true)
        @PathParam("sourceProjectId")
        sourceProjectId: String,
        @Parameter(description = "目标项目ID", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "源流水线组名称，为空则复制源项目下全部流水线组", required = false)
        @QueryParam("viewName")
        viewName: String?
    ): Result<Boolean>
}
