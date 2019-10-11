package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.RuleIndicatorSet
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorListResponse
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorStageGroup
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_INDICATOR_V2"], description = "质量红线-指标")
@Path("/user/indicators/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityIndicatorResource {

    @ApiOperation("获取对应控制点所有指标")
    @Path("/project/{projectId}/listIndicators")
    @GET
    fun listIndicators(
        @PathParam("projectId")
        projectId: String
    ): Result<List<IndicatorStageGroup>>

    @ApiOperation("获取所有指标集")
    @Path("/listIndicatorSet")
    @GET
    fun listIndicatorSet(): Result<List<RuleIndicatorSet>>

    @ApiOperation("指标列表页面接口")
    @Path("/project/{projectId}/queryIndicatorList")
    @GET
    fun queryIndicatorList(
        @PathParam("projectId")
        projectId: String,
        @QueryParam("keyword")
        keyword: String?
    ): Result<IndicatorListResponse>

    @ApiOperation("获取单个指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/get")
    @GET
    fun get(
        @PathParam("projectId")
        projectId: String,
        @PathParam("indicatorId")
        indicatorId: String
    ): Result<QualityIndicator>

    @ApiOperation("创建指标")
    @Path("/project/{projectId}/create")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("指标请求报文", required = true)
        indicatorCreate: IndicatorCreate
    ): Result<Boolean>

    @ApiOperation("更新指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/update")
    @PUT
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("指标ID", required = true)
        @PathParam("indicatorId")
        indicatorId: String,
        @ApiParam("指标请求报文", required = true)
        indicatorCreate: IndicatorCreate
    ): Result<Boolean>

    @ApiOperation("删除指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/delete")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("指标hash ID", required = true)
        @PathParam("indicatorId")
        indicatorId: String
    ): Result<Boolean>
}