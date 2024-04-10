package com.tencent.devops.dispatch.devcloud.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceOptionsVO
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
import javax.ws.rs.core.MediaType

@Tag(name = "OP_DISPATCH_DEVCLOUD", description = "OP-DEVCLOUD构建机性能配置接口")
@Path("/op/dispatchDevcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPDcPerformanceOptionsResource {

    @GET
    @Path("/performanceOptions/list")
    @Operation(summary = "获取devcloud性能基础配置列表")
    fun listDcPerformanceOptions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<PerformanceOptionsVO>>

    @POST
    @Path("/performanceOptions/add")
    @Operation(summary = "新增性能基础配置")
    fun createDcPerformanceOptions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "性能配置", required = true)
        performanceOptionsVO: PerformanceOptionsVO
    ): Result<Boolean>

    @PUT
    @Path("/performanceOptions/{id}/update")
    @Operation(summary = "更新性能基础配置")
    fun updateDcPerformanceOptions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "配置ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "性能配置", required = true)
        performanceOptionsVO: PerformanceOptionsVO
    ): Result<Boolean>

    @DELETE
    @Path("/performanceOptions/delete/{id}")
    @Operation(summary = "删除Devcloud性能基础配置")
    fun deleteDcPerformanceOptions(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "ID", required = true)
        @PathParam("id")
        projectId: Long
    ): Result<Boolean>
}
