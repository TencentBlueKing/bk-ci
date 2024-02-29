package com.tencent.devops.dispatch.devcloud.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.ListPage
import com.tencent.devops.dispatch.devcloud.pojo.performance.OPPerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OP_DISPATCH_DEVCLOUD", description = "OP-DEVCLOUD构建机性能配置接口")
@Path("/op/dispatchDevcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPDcPerformanceConfigResource {

    @GET
    @Path("/performanceConfig/list")
    @Operation(summary = "获取devcloud性能配置列表")
    fun listDcPerformanceConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ListPage<PerformanceConfigVO>>

    @POST
    @Path("/performanceConfig/add")
    @Operation(summary = "新增性能配置")
    fun createDcPerformanceConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "性能配置", required = true)
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Result<Boolean>

    @PUT
    @Path("/performanceConfig/{projectId}/update")
    @Operation(summary = "更新Devcloud性能配置")
    fun updateDcPerformanceConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "性能配置", required = true)
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Result<Boolean>

    @DELETE
    @Path("/performanceConfig/delete/{projectId}")
    @Operation(summary = "删除Devcloud性能配置")
    fun deleteDcPerformanceConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
