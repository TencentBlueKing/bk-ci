package com.tencent.devops.auth.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_USER_MANAGE", description = "权限-op-用户管理")
@Path("/op/auth/user/manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpUserManageResource {
    @POST
    @Path("/syncUserInfoData/")
    @Operation(summary = "同步用户数据")
    fun syncUserInfoData(): Result<Boolean>

    @POST
    @Path("/syncDepartmentInfoData/")
    @Operation(summary = "同步部门数据")
    fun syncDepartmentInfoData(): Result<Boolean>

    @POST
    @Path("/syncDepartmentRelations/")
    @Operation(summary = "同步部门关系数据")
    fun syncDepartmentRelations(): Result<Boolean>
}
