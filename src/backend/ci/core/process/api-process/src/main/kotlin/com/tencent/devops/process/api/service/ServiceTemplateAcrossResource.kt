package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

@Api(tags = ["SERVICE_TEMPLATE_ACROSS"], description = "服务-模板跨项目使用资源")
@Path("/service/templates/across")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTemplateAcrossResource {

    @ApiOperation("批量创建当前构建中的跨项目模板信息")
    @POST
    @Path("")
    fun batchCreate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "跨项目模板信息", required = true)
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    )

    @ApiOperation("获取当前构建中的跨项目模板信息")
    @GET
    @Path("")
    fun getBuildAcrossTemplateInfo(
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String
    ): Result<List<BuildTemplateAcrossInfo>>

    @ApiOperation("修改跨项目模板信息")
    @PUT
    @Path("")
    fun update(
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @ApiParam(value = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<Boolean>

    @ApiOperation("删除跨项目模板信息")
    @DELETE
    @Path("")
    fun delete(
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @ApiParam(value = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String?
    ): Result<Boolean>
}
