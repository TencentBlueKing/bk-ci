package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildId
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_PIPELINE"], description = "外部-远程触发流水线")
@Path("/external/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalPipelineResource {

    @ApiOperation("远程触发构建")
    @POST
    @Path("/{token}/build")
    fun remoteBuild(
        @ApiParam("远程认证信息", required = true)
        @PathParam("token")
        token: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>
    ): Result<BuildId>

    @ApiOperation("获取流水线徽章")
    @Produces("image/svg+xml") // 只显示，不下载
    @GET
    @Path("projects/{projectId}/{pipelineId}/badge")
    fun getBadge(
        @ApiParam("项目IS", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): String
}