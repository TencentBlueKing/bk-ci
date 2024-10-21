package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_TEMPLATE_ACROSS", description = "服务-模板跨项目使用资源")
@Path("/service/templates/across")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTemplateAcrossResource {

    @Operation(summary = "批量创建当前构建中的跨项目模板信息")
    @POST
    @Path("")
    fun batchCreate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "跨项目模板信息", required = true)
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    )

    @Operation(summary = "获取当前构建中的跨项目模板信息")
    @GET
    @Path("")
    fun getBuildAcrossTemplateInfo(
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String
    ): Result<List<BuildTemplateAcrossInfo>>

    @Operation(summary = "修改跨项目模板信息")
    @PUT
    @Path("")
    fun update(
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<Boolean>

    @Operation(summary = "删除跨项目模板信息")
    @DELETE
    @Path("")
    fun delete(
        @Parameter(description = "projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String?
    ): Result<Boolean>
}
