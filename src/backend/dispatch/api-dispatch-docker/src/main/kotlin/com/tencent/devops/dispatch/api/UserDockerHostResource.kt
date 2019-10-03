package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.DebugStartParam
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
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_DOCKER_HOST"], description = "用户-获取构建容器信息")
@Path("/user/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDockerHostResource {

    @ApiOperation("启动调试容器")
    @POST
    @Path("/startDebug")
    fun startDebug(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        debugStartParam: DebugStartParam
    ): Result<Boolean>?

    @ApiOperation("根据pipelineId和vmSeqId获取容器信息")
    @GET
    @Path("/getDebugStatus/{projectId}/{pipelineId}/{vmSeqId}")
    fun getDebugStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<ContainerInfo>?

    @ApiOperation("终止调试容器")
    @POST
    @Path("/stopDebug/{projectId}/{pipelineId}/{vmSeqId}")
    fun stopDebug(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>?

    @ApiOperation("根据buildId获取容器信息")
    @GET
    @Path("/getContainerInfo/{projectId}/{pipelineId}/{buildId}/{vmSeqId}")
    fun getContainerInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("容器Id", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<ContainerInfo>?

    @ApiOperation("获取灰度开启webConsole的项目")
    @GET
    @Path("/getGreyWebConsoleProject")
    fun getGreyWebConsoleProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<String>>

    @ApiOperation("清理流水线上次使用的构建机信息")
    @POST
    @Path("/cleanIp/{projectId}/{pipelineId}/{vmSeqId}")
    fun cleanIp(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>?
}