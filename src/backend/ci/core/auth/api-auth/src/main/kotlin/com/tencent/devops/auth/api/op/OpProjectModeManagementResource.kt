package com.tencent.devops.auth.api.op

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.auth.pojo.request.BatchSetProjectModesRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "PROJECT_MODE_MANAGEMENT", description = "项目权限路由模式管理")
@Path("/op/project/mode/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectModeManagementResource {

    @POST
    @Path("/{projectCode}/setMode/")
    @Operation(summary = "设置项目路由模式")
    fun setProjectMode(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "路由模式", required = true)
        @QueryParam("mode")
        mode: String
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/removeMode/")
    @Operation(summary = "删除项目路由模式配置")
    fun removeProjectMode(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @POST
    @Path("/batchSetModes/")
    @Operation(summary = "批量设置项目路由模式")
    fun batchSetProjectModes(
        @Parameter(description = "批量设置请求", required = true)
        batchRequest: BatchSetProjectModesRequest
    ): Result<Boolean>

    @POST
    @Path("/setDefaultMode/")
    @Operation(summary = "设置默认路由模式")
    fun setDefaultMode(
        @Parameter(description = "默认路由模式", required = true)
        @QueryParam("mode")
        mode: String
    ): Result<Boolean>

    @POST
    @Path("/clearAllModes/")
    @Operation(summary = "清空所有项目路由模式配置")
    fun clearAllProjectModes(
        @Parameter(description = "确认操作", required = true)
        @QueryParam("confirm")
        confirm: Boolean
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/getMode/")
    @Operation(summary = "获取项目路由模式")
    fun getProjectMode(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<RoutingMode?>

    @POST
    @Path("/getDefaultMode/")
    @Operation(summary = "获取默认路由模式")
    fun getDefaultMode(): Result<RoutingMode>
}
