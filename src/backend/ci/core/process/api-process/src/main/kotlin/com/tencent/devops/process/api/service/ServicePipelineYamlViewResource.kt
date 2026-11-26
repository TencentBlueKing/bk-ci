package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCleanupReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCreateReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewGroupUpdateReq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PAC_PIPELINE_YAML_VIEW", description = "服务-PAC流水线组")
@Path("/service/pipeline/yaml/view")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineYamlViewResource {

    @Operation(summary = "PAC 不存在时创建流水线组")
    @POST
    @Path("/projects/{projectId}/createYamlViewIfAbsent")
    fun createYamlViewIfAbsent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "创建请求", required = true)
        request: PipelineYamlViewCreateReq
    ): Result<Boolean>

    @Operation(summary = "PAC 删除流水线后清理流水线组")
    @POST
    @Path("/projects/{projectId}/cleanupYamlView")
    fun cleanupYamlView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "清理请求", required = true)
        request: PipelineYamlViewCleanupReq
    ): Result<Boolean>

    @Operation(summary = "PAC 流水线变更后刷新流水线组")
    @POST
    @Path("/projects/{projectId}/updateGroupAfterPipelineUpdate")
    fun updateGroupAfterPipelineUpdate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "刷新请求", required = true)
        request: PipelineYamlViewGroupUpdateReq
    ): Result<Boolean>
}
