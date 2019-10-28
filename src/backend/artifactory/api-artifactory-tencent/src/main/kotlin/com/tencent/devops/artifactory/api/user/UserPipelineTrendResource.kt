package com.tencent.devops.artifactory.api.user

import com.tencent.devops.artifactory.pojo.TrendInfoDto
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_ARTIFACTORY_PIPELINE_TREND"], description = "版本仓库-构建产物")
//@Path("/user/pipelineTrend")
@Path("/user/pipeline/artifactory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineTrendResource {

    @ApiOperation("APK,IPA趋势图")
    //@Path("/pipelines{pipelineId}/trend")
    @Path("/construct/{pipelineId}/trend")
    @GET
    fun constructApkAndIpaTrend(
        @ApiParam("流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("起始时间", required = true)
        @QueryParam("startTime")
        startTime: Long,
        @ApiParam("结束时间", required = true)
        @QueryParam("endTime")
        endTime: Long,
        @ApiParam("页数", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页多少条", required = false, defaultValue = "1000")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<TrendInfoDto>
}