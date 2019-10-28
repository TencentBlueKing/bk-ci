package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_PIPELINE_VIEW"], description = "app流水线视图相关接口")
@Path("/app/pipelineViews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppPipelineViewResource {

    @ApiOperation("app获取视图流水线编排列表")
    @GET
    //@Path("/projects/{projectId}/listViewPipelines")
    @Path("/projects/{projectId}/listViewPipelines")
    fun listViewPipelines(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @ApiParam("按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?,
        @ApiParam("按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String?,
        @ApiParam("按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String?,
        @ApiParam("用户视图ID", required = false)
        @QueryParam("viewId")
        viewId: String
    ): Result<PipelineViewPipelinePage<Pipeline>>

    @ApiOperation("获取视图设置")
    @GET
    //@Path("/projects/{projectId}/settings")
    @Path("/projects/{projectId}/settings")
    fun getViewSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewSettings>
}