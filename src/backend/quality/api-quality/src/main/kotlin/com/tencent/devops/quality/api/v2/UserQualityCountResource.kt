package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityRuleIntercept
import com.tencent.devops.quality.api.v2.pojo.response.CountOverviewResponse
import com.tencent.devops.quality.pojo.CountDailyIntercept
import com.tencent.devops.quality.pojo.CountPipelineIntercept
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

@Api(tags = ["USER_COUNT_V2"], description = "质量红线-统计v2")
@Path("/user/counts/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityCountResource {
    @ApiOperation("查看总览")
    @Path("/{projectId}/overview")
    @GET
    fun getOverview(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<CountOverviewResponse>

    @ApiOperation("查看流水线拦截")
    @Path("/{projectId}/pipelineIntercept")
    @GET
    fun getPipelineIntercept(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<CountPipelineIntercept>>

    @ApiOperation("查看每日拦截")
    @Path("/{projectId}/dailyIntercept")
    @GET
    fun getDailyIntercept(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<CountDailyIntercept>>

    @ApiOperation("查看最近拦截历史")
    @Path("/{projectId}/ruleIntercept")
    @GET
    fun getRuleIntercept(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Page<QualityRuleIntercept>>
}