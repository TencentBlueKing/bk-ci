package com.tencent.devops.auth.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "AUTH_SERVICE_RESOURCE_GROUP", description = "权限--用户组相关")
@Path("/service/auth/resource/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceResourceGroupResource {
    @POST
    @Path("/{projectCode}/createGroupByGroupCode/")
    @Operation(summary = "根据groupCode添加用户组")
    fun createGroupByGroupCode(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组code,CI管理员为CI_MANAGER", required = true)
        @QueryParam("groupCode")
        groupCode: BkAuthGroup
    ): Result<Boolean>

    @DELETE
    @Path("/{projectCode}/deleteGroup/")
    @Operation(summary = "删除用户组")
    fun deleteGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组ID", required = true)
        @QueryParam("groupId")
        groupId: Int
    ): Result<Boolean>
}
