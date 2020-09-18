package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE_AUTH"], description = "服务-流水线-权限中心")
@Path("/service/auth/pipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthPipelineResource {

    @ApiOperation("流水线编排列表")
    @GET
    @Path("/{projectId}/list")
    fun pipelineList(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int? = null,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @ApiOperation("流水线信息")
    @GET
    @Path("/getInfos")
    fun pipelineInfos(
        @ApiParam("ID集合", required = true)
        @QueryParam("pipelineIds")
        pipelineIds: Set<String>
    ): Result<List<SimplePipeline>?>
}