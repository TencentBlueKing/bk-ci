package com.tencent.devops.auth.api.op

import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
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

@Tag(name = "OP_AUTH_FACADE", description = "op操作门面类")
@Path("/op/auth/facade/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPermissionFacadeResource {
    @POST
    @Path("/{projectId}/createCustomGroupAndPermissions/")
    @Operation(summary = "创建自定义用户组和权限")
    fun createCustomGroupAndPermissions(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "自定义组创建请求体", required = true)
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int>

    @POST
    @Path("/{projectId}/grantAllProjectGroupsPermission/")
    @Operation(summary = "授予项目级用户组权限，例子：给项目级用户组都添加上流水线列表权限。")
    fun grantAllProjectGroupsPermission(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "操作列表", required = true)
        actions: List<String>
    ): Result<Boolean>
}
