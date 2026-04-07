package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyCreateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyReorderReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyUpdateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_DISPATCH_ENV_STRATEGY", description = "环境调度策略")
@Path("/user/dispatch/envStrategy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDispatchEnvStrategyResource {

    @Operation(summary = "获取环境的调度策略列表")
    @GET
    @Path("/projects/{projectId}/envs/{envId}/strategies")
    fun listStrategies(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long
    ): Result<List<DispatchEnvStrategyVO>>

    @Operation(summary = "创建自定义调度策略")
    @POST
    @Path("/projects/{projectId}/envs/{envId}/strategies")
    fun createStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long,
        @Parameter(description = "创建请求", required = true)
        request: DispatchEnvStrategyCreateReq
    ): Result<Long>

    @Operation(summary = "更新调度策略")
    @PUT
    @Path("/projects/{projectId}/envs/{envId}/strategies/{strategyId}")
    fun updateStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long,
        @Parameter(description = "策略ID", required = true)
        @PathParam("strategyId")
        strategyId: Long,
        @Parameter(description = "更新请求", required = true)
        request: DispatchEnvStrategyUpdateReq
    ): Result<Boolean>

    @Operation(summary = "删除调度策略")
    @DELETE
    @Path("/projects/{projectId}/envs/{envId}/strategies/{strategyId}")
    fun deleteStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long,
        @Parameter(description = "策略ID", required = true)
        @PathParam("strategyId")
        strategyId: Long
    ): Result<Boolean>

    @Operation(summary = "批量删除调度策略")
    @DELETE
    @Path("/projects/{projectId}/envs/{envId}/strategies")
    fun batchDeleteStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long,
        @Parameter(description = "策略ID列表", required = true)
        strategyIds: Set<Long>
    ): Result<Boolean>

    @Operation(summary = "调整调度策略排序")
    @POST
    @Path("/projects/{projectId}/envs/{envId}/strategies/reorder")
    fun reorderStrategies(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境ID", required = true)
        @PathParam("envId")
        envId: Long,
        @Parameter(description = "排序请求", required = true)
        request: DispatchEnvStrategyReorderReq
    ): Result<Boolean>
}
