package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.app.pipeline.AppPipeline
import com.tencent.devops.process.pojo.app.pipeline.AppPipelineHistory
import com.tencent.devops.process.pojo.app.pipeline.AppProject
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

@Api(tags = ["APP_PIPELINE"], description = "app流水线相关接口")
@Path("/app/pipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppPipelineResource {

    @ApiOperation("列出用户的所有项目")
    @GET
    //@Path("/projects")
    @Path("/project/list")
    fun listProjects(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Page<AppProject>>

    @ApiOperation("列出用户某项目下的所有流水线")
    @GET
    //@Path("/projects/{projectId}/listPipelines")
    @Path("/{projectId}/listPipelines")
    fun listPipelines(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME
    ): Result<PipelinePage<AppPipeline>>

    @ApiOperation("列出用户某项目下的流水线的所有构建历史")
    @GET
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    @Path("/{projectId}/{pipelineId}/history")
    fun listPipelineHistory(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @ApiParam("分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>?
    ): Result<Page<AppPipelineHistory>>

    @ApiOperation("获取流水线构建中的查询条件-分支")
    @GET
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/branchName")
    @Path("/{projectId}/{pipelineId}/historyCondition/branchName")
    fun getHistoryConditionBranch(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("仓库", required = false)
        @QueryParam("alias")
        alias: List<String>?
    ): Result<List<String>>

    @ApiOperation("列出用户某项目下的流水线的所有收藏")
    @GET
    //@Path("/collections")
    @Path("/collect/list")
    fun listUserCollect(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AppPipeline>>

    @ApiOperation("收藏流水线")
    @POST
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/collect")
    @Path("/{projectId}/{pipelineId}/collect")
    fun collectPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "是否收藏（false表示取消收藏）", required = true)
        @QueryParam("isCollect")
        isCollect: Boolean
    ): Result<Boolean>
}