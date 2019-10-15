package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_ARCHIVE"], description = "服务-流水线资源")
@Path("/user/archive/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserArchivePipelineResource {

    @ApiOperation("获取某个项目的所有流水线")
    @GET
    @Path("/projects/{projectId}/getAllPipelines")
    @Deprecated("use getDownloadAllPipelines instead")
    fun getAllPipelines(
        @ApiParam(value = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String
    ): Result<List<Map<String, String>>>

    @ApiOperation("获取某条流水线所有构建号")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/getAllBuildNo")
    fun getAllBuildNo(
        @ApiParam(value = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线id", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String
    ): Result<List<Map<String, String>>>

    @ApiOperation("获取某个项目用户可以下载归档的所有流水线")
    @GET
    @Path("/projects/{projectId}/getDownloadAllPipelines")
    fun getDownloadAllPipelines(
        @ApiParam(value = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String
    ): Result<List<Map<String, String>>>
}