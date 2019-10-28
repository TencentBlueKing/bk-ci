package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.Pipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_PIPELINE"], description = "服务-流水线资源")
@Path("/build/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildPipelineResource {
    @ApiOperation("根据流水线id获取流水线名字")
    @POST
    //@Path("/projects/{projectId}/getPipelineNames")
    @Path("/{projectId}/getPipelineNames")
    fun getPipelineNameByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("获取流水线构建历史")
    @GET
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    @Path("/{projectId}/{pipelineId}/history")
    fun getHistoryBuild(
        @ApiParam(
            value = "当前流水线的buildId获取相关buildId进行鉴权",
            required = true,
            defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @QueryParam("currentBuildId")
        currentBuildId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<BuildHistoryPage<BuildHistory>>

    @ApiOperation("列出所有流水线")
    @GET
    //@Path("/projects/{projectId}/listAllPipelines")
    @Path("/{projectId}/")
    fun list(
        @ApiParam(
            value = "当前流水线的buildId获取相关buildId进行鉴权",
            required = true,
            defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @QueryParam("currentBuildId")
        currentBuildId: String,
        @ApiParam("原子类型", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("筛选流水线逗号分隔", required = false)
        @QueryParam("pipelineIdList")
        pipelineIdListString: String?
    ): Result<List<Pipeline>>
}