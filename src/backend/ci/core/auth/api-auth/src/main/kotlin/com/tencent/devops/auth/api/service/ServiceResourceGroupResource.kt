package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
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
    @GET
    @Path("/{projectCode}/{groupId}/getGroupPermissionDetail")
    @Operation(summary = "查询用户组权限详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].actionId}"], responseDataCacheFlag = true)
    fun getGroupPermissionDetail(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户组ID")
        @PathParam("groupId")
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>>

    @POST
    @Path("/{projectCode}/createGroupByGroupCode/")
    @Operation(summary = "根据groupCode添加用户组")
    fun createGroupByGroupCode(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组code,CI管理员为CI_MANAGER", required = true)
        @QueryParam("groupCode")
        groupCode: BkAuthGroup
    ): Result<Int>

    @POST
    @Path("/{projectCode}/createCustomGroupAndPermissions/")
    @Operation(summary = "创建自定义用户组和权限")
    fun createCustomGroupAndPermissions(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "自定义组创建请求体", required = true)
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int>

    @POST
    @Path("/{projectCode}/createGroup/")
    @Operation(summary = "创建自定义组(不包含权限，空权限组)")
    fun createGroup(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "创建组DTO", required = true)
        groupAddDTO: GroupAddDTO
    ): Result<Int>

    @DELETE
    @Path("/{projectCode}/deleteGroup/")
    @Operation(summary = "删除用户组")
    fun deleteGroup(
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
