package com.tencent.devops.process.api.quality

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.quality.pojo.PipelineListRequest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.quality.QualityPipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_QUALITY_PIPELINE"], description = "用户-质量红线-流水线资源")
@Path("/user/quality/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityPipelineResource {

    @ApiOperation("用户获取视图流水线编排列表")
    @GET
    @Path("/projects/{projectId}/listViewPipelines")
    fun listViewPipelines(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线名称关键字", required = false)
        @QueryParam("keywords")
        keywords: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("用户视图ID", required = false)
        @QueryParam("viewId")
        viewId: String?
    ): Result<PipelineViewPipelinePage<QualityPipeline>>

    @ApiOperation("获取单个流水线信息接口")
    @GET
    @Path("/project/{projectId}/pipeline/{pipelineId}/getPipelineInfo")
    fun getPipelineInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("渠道号", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<PipelineInfo>

    @ApiOperation("根据id列出流水线")
    @POST
    @Path("/{projectId}/")
    fun list(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("原子类型", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        request: PipelineListRequest?
    ): Result<List<Pipeline>>
}