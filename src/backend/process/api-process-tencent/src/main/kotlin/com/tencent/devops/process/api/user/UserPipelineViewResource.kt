package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewCreate
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineNewViewUpdate
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
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
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_VIEW"], description = "用户-流水线视图")
@Path("/user/pipelineViews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineViewResource {
    @ApiOperation("获取视图设置")
    @GET
    @Path("/projects/{projectId}/settings")
    fun getViewSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewSettings>

    @ApiOperation("更新视图设置")
    @POST
    @Path("/projects/{projectId}/settings")
    fun updateViewSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        viewIdList: List<String>
    ): Result<Boolean>

    @ApiOperation("获取所有视图")
    @GET
    @Path("/projects/{projectId}/")
    fun getViews(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineNewViewSummary>>

    @ApiOperation("添加视图")
    @POST
    @Path("/projects/{projectId}/")
    fun addView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineNewViewCreate
    ): Result<PipelineViewId>

    @ApiOperation("获取视图")
    @GET
    @Path("/projects/{projectId}/views/{viewId}")
    fun getView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<PipelineNewView>

    @ApiOperation("删除视图")
    @DELETE
    @Path("/projects/{projectId}/views/{viewId}")
    fun deleteView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<Boolean>

    @ApiOperation("更改视图")
    @PUT
    @Path("/projects/{projectId}/views/{viewId}")
    fun updateView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String,
        pipelineView: PipelineNewViewUpdate
    ): Result<Boolean>
}