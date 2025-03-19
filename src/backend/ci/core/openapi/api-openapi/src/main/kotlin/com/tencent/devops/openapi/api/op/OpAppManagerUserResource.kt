package com.tencent.devops.openapi.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.AppManagerInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_APP_MANAGER_INFO", description = "OP-AppCode管理员")
@Path("/op/appManager/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpAppManagerUserResource {

    @Operation(summary = "设置appCode的管理员")
    @POST
    @Path("/{appCode}")
    fun setGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @Parameter(description = "appCode", required = true)
        @PathParam("appCode")
        appCode: String,
        @Parameter(description = "appManagerInfo", required = true)
        appManagerInfo: AppManagerInfo
    ): Result<Boolean>

    @Operation(summary = "获取appCode的管理员")
    @GET
    @Path("/{appCode}")
    fun getGroup(
        @Parameter(description = "appCode", required = true)
        @PathParam("appCode")
        appCode: String
    ): Result<String?>

    @Operation(summary = "删除appCode的管理员")
    @DELETE
    @Path("/ids/{id}")
    fun deleteProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: Int
    ): Result<Boolean>
}
