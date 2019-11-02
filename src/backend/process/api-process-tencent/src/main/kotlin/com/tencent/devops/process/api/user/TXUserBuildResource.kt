package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.coverity.CodeccReport
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

@Api(tags = ["USER_BUILD"], description = "用户-构建资源")
@Path("/user/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXUserBuildResource {

    @ApiOperation("获取CodeCC报告")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/codeccReport")
    @Path("/{projectId}/{pipelineId}/codeccReport")
    fun getCodeccReport(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<CodeccReport>

    @ApiOperation("质量红线人工审核")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/elements/{elementId}/qualityGateReview/{action}")
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/qualityGateReview/{action}")
    fun manualQualityGateReview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("步骤Id", required = true)
        @PathParam("elementId")
        elementId: String,
        @ApiParam("动作", required = true)
        @PathParam("action")
        action: ManualReviewAction
    ): Result<Boolean>
}
