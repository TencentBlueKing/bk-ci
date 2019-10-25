package com.tencent.devops.openapi.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.BuildStatisticsResponse
import com.tencent.devops.process.pojo.Pipeline
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

@Api(tags = ["OPEN_API_V2_PIPELINE"], description = "OPEN-API-V2-流水线资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPipelineResourceV2 {
    @ApiOperation("获取组织的流水线列表")
    @GET
    @Path("/organization")
    fun getListByOrganization(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_NAME)
        organizationName: String,
        @ApiParam("部门名称", required = false, defaultValue = "")
        @QueryParam("deptName")
        deptName: String? = "",
        @ApiParam("中心名称", required = false, defaultValue = "")
        @QueryParam("centerName")
        centerName: String? = "",
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<Pipeline>>

    @ApiOperation("获取使用指定构建资源的流水线列表")
    @GET
    @Path("/buildResource")
    fun getListByBuildResource(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "构建类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_TYPE)
        buildResourceType: String,
        @ApiParam("构建资源值（镜像名称/节点ID/环境ID）", required = false, defaultValue = "")
        @QueryParam("resourceValue")
        buildResourceValue: String? = "",
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<Pipeline>>

    @ApiOperation("获取流水线构建结果统计数据")
    @GET
    @Path("/builds/statistics")
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
