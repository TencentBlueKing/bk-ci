package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
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

@Api(tags = ["APP_PIPELINE_GROUP"], description = "APP-流水线分组")
@Path("/app/pipelineGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppPipelineGroupResource {

    @ApiOperation("获取所有分组信息")
    @GET
    @Path("/groups")
    fun getGroups(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<PipelineGroup>>

    @ApiOperation("添加分组")
    @POST
    @Path("/groups/")
    fun addGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineGroup: PipelineGroupCreate
    ): Result<Boolean>

    @ApiOperation("更改分组")
    @PUT
    @Path("/groups/")
    fun updateGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineGroup: PipelineGroupUpdate
    ): Result<Boolean>

    @ApiOperation("删除分组")
    @DELETE
    @Path("/groups/")
    fun deleteGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("分组ID", required = true)
        @QueryParam("groupId")
        groupId: String
    ): Result<Boolean>

    @ApiOperation("添加标签")
    @POST
    @Path("/labels/")
    fun addLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineLabel: PipelineLabelCreate
    ): Result<Boolean>

    @ApiOperation("删除标签")
    @DELETE
    @Path("/labels/")
    fun deleteLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("标签ID", required = true)
        @QueryParam("labelId")
        labelId: String
    ): Result<Boolean>

    @ApiOperation("更改标签")
    @PUT
    @Path("/labels/")
    fun updateLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineLabel: PipelineLabelUpdate
    ): Result<Boolean>
}