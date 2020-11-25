package com.tencent.devops.openapi.api.apigw.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogs
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_LOG"], description = "OPEN-API-V2-LOG日志")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v2/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwLogResourceV2 {
    @ApiOperation("根据构建ID获取初始化所有日志")
    @GET
    @Path("/projectIds/{projectId}/pipelineIds/{pipelineId}/buildIds/{buildId}")
    fun getInitLogs(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否请求分析日志", required = false)
        @QueryParam("isAnalysis")
        isAnalysis: Boolean? = false,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("queryKeywords")
        queryKeywords: String?,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        elementId: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>
}