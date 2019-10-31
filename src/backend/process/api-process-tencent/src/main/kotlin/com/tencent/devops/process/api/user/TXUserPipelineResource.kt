package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE"], description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXUserPipelineResource {

    @ApiOperation("项目是否灰度Docker构建方案")
    @GET
    //@Path("/projects/{projectId}/enableDocker")
    @Path("/{projectId}/enableDocker")
    fun enableDockerBuild(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("订阅流水线消息")
    @PUT
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/subscription")
    @Path("/{projectId}/{pipelineId}/subscription")
    fun subscription(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("消息订阅类型", required = false)
        @QueryParam("type")
        type: SubscriptionType? = SubscriptionType.ALL
    ): Result<Boolean>

    @ApiOperation("获取是否订阅流水线")
    @GET
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/subscription")
    @Path("/{projectId}/{pipelineId}/subscription")
    fun getSubscription(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineSubscription?>

    @ApiOperation("取消订阅流水线消息")
    @DELETE
    //@Path("/projects/{projectId}/pipelines/{pipelineId}/subscription")
    @Path("/{projectId}/{pipelineId}/subscription")
    fun cancelSubscription(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>
}