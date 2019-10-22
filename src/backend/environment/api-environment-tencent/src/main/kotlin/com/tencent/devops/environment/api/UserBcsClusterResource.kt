package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.BcsCluster
import com.tencent.devops.environment.pojo.BcsImageInfo
import com.tencent.devops.environment.pojo.BcsVmModel
import com.tencent.devops.environment.pojo.ProjectInfo
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

@Api(tags = ["USER_CLUSTER"], description = "用户-BCS集群信息")
@Path("/user/bcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserBcsClusterResource {
    @ApiOperation("获取BCS集群列表")
    @GET
    @Path("/getClusterList")
    fun getClusterList(): Result<List<BcsCluster>>

    @ApiOperation("获取Bcs镜像列表")
    @GET
    @Path("/projects/{projectId}/getImageList")
    fun getImageList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<BcsImageInfo>>

    @ApiOperation("获取VM机型列表")
    @GET
    @Path("/projects/{projectId}/getVmModelList")
    fun getVmModelList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<BcsVmModel>>

    @ApiOperation("获取VM配额信息")
    @GET
    @Path("/projects/{projectId}/getProjectInfo")
    fun getProjectInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ProjectInfo>
}