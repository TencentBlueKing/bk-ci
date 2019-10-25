package com.tencent.devops.openapi.api.external

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.BuildStatisticsResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Created by ddlin on 2018/02/06.
 * Powered By Tencent
 */
@Api(tags = ["SERVICE_MEASURE"], description = "服务-度量资源")
@Path("/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMeasureResource {

    @ApiOperation("获取流水线构建结果统计数据")
    @GET
    @Path("/pipelines/builds/statistics")
    fun buildStatistics(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Int,
        @ApiParam("部门名称", required = false, defaultValue = "")
        @QueryParam("deptName")
        deptName: String? = "",
        @ApiParam("中心名称", required = false, defaultValue = "")
        @QueryParam("centerName")
        centerName: String? = "",
        @ApiParam("起始时间", required = false, defaultValue = "")
        @QueryParam("beginTime")
        beginTime: String? = "",
        @ApiParam("截止时间", required = false, defaultValue = "")
        @QueryParam("endTime")
        endTime: String? = "",
        @ApiParam("类型（ALL/CONTAINS_SCRIPT/CONTAINS_CODECC）", required = false, defaultValue = "ALL")
        @QueryParam("type")
        type: String? = ""
    ): Result<BuildStatisticsResponse>
}