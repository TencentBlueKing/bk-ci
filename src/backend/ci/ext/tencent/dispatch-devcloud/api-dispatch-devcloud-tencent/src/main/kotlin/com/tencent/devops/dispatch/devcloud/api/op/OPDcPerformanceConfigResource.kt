package com.tencent.devops.dispatch.devcloud.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.ListPage
import com.tencent.devops.dispatch.devcloud.pojo.performance.OPPerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
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

@Api(tags = ["OP_DISPATCH_DEVCLOUD"], description = "OP-DEVCLOUD构建机性能配置接口")
@Path("/op/dispatchDevcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPDcPerformanceConfigResource {

    @GET
    @Path("/performanceConfig/list")
    @ApiOperation("获取devcloud性能配置列表")
    fun listDcPerformanceConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ListPage<PerformanceConfigVO>>

    @POST
    @Path("/performanceConfig/add")
    @ApiOperation("新增性能配置")
    fun createDcPerformanceConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("性能配置", required = true)
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Result<Boolean>

    @PUT
    @Path("/performanceConfig/{projectId}/update")
    @ApiOperation("更新Devcloud性能配置")
    fun updateDcPerformanceConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("性能配置", required = true)
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Result<Boolean>

    @DELETE
    @Path("/performanceConfig/delete/{projectId}")
    @ApiOperation("删除Devcloud性能配置")
    fun deleteDcPerformanceConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
