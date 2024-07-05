package com.tencent.devops.auth.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_SERVICE_RESOURCE", description = "权限--资源相关接口")
@Path("/open/service/auth/resource/member")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceResourceMemberResource {
    /**
     * @param resourceType 是个枚举类型详见 AuthResourceType
     * @see AuthResourceType
     */
    @GET
    @Path("/{projectCode}/getResourceGroupUsers")
    @Operation(summary = "获取特定资源下用户组成员")
    fun getResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String,
        @QueryParam("group")
        @Parameter(description = "资源用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/getResourceUsers")
    @Operation(summary = "拉取资源下所有成员，并按项目角色组分组成员信息返回")
    fun getResourceGroupAndMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String
    ): Result<List<BkAuthGroupAndUserList>>

    @POST
    @Path("/{projectCode}/batchAddResourceGroupMembers/")
    @Operation(summary = "用户组添加成员")
    fun batchAddResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "用户组添加成员请求体", required = true)
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @DELETE
    @Path("/{projectCode}/batchDeleteResourceGroupMembers/")
    @Operation(summary = "用户组删除成员")
    fun batchDeleteResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "用户组删除成员请求体", required = true)
        projectDeleteUserInfo: ProjectDeleteUserInfo
    ): Result<Boolean>
}
