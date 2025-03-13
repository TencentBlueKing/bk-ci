package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * remotedev依赖的project，所以关于project与remotedev相关的接口应该放到remotedev
 */
@Tag(name = "OP_PROJECT_WORKSPACE", description = "OP_PROJECT_WORKSPACE")
@Path("/op/remotedev/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectResource {

    @Operation(summary = "开启或关闭云研发")
    @POST
    @Path("/enableOrDisable")
    fun enableOrDisableRemotedev(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "是否开启", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "迁移旧数据")
    @POST
    @Path("/migrateOldData")
    fun migrateOldData(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Boolean>

    @Operation(summary = "存量实例vmName")
    @POST
    @Path("/updateVmName")
    fun updateVmName(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        projectId: Set<String>
    ): Result<Boolean>
}
