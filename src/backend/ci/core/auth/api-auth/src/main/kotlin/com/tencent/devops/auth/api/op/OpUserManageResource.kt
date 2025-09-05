package com.tencent.devops.auth.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_USER_MANAGE", description = "权限-op-用户管理")
@Path("/op/auth/user/manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpUserManageResource {
    @POST
    @Path("/syncAllUserInfoData/")
    @Operation(summary = "全量同步用户数据")
    fun syncAllUserInfoData(): Result<Boolean>

    @POST
    @Path("/syncUserInfoData/")
    @Operation(summary = "同步用户数据")
    fun syncUserInfoData(
        userIds: List<String>
    ): Result<Boolean>

    @POST
    @Path("/syncDepartmentInfoData/")
    @Operation(summary = "同步部门数据")
    fun syncDepartmentInfoData(): Result<Boolean>

    @POST
    @Path("/syncDepartmentRelations/")
    @Operation(summary = "同步部门关系数据")
    fun syncDepartmentRelations(): Result<Boolean>

    @POST
    @Path("/{projectId}/resetResourceAuthorization")
    @Operation(summary = "重置资源授权管理")
    fun resetResourceAuthorization(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源授权交接条件实体", required = true)
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>>
}
