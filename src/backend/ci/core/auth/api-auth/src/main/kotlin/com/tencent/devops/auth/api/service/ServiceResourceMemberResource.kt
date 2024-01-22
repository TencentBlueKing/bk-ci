package com.tencent.devops.auth.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_SERVICE_RESOURCE"], description = "权限--资源相关接口")
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
    @ApiOperation("获取特定资源下用户组成员")
    fun getResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源code", required = false)
        resourceCode: String,
        @QueryParam("group")
        @ApiParam("资源用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/getResourceUsers")
    @ApiOperation("拉取资源下所有成员，并按项目角色组分组成员信息返回")
    fun getResourceGroupAndMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源code", required = false)
        resourceCode: String
    ): Result<List<BkAuthGroupAndUserList>>

    @POST
    @Path("/{projectCode}/batchAddResourceGroupMembers/")
    @ApiOperation("用户组添加成员")
    fun batchAddResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @ApiParam("用户组添加成员请求体", required = true)
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @DELETE
    @Path("/{projectCode}/batchDeleteResourceGroupMembers/")
    @ApiOperation("用户组删除成员")
    fun batchDeleteResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @ApiParam("用户组删除成员请求体", required = true)
        projectDeleteUserInfo: ProjectDeleteUserInfo
    ): Result<Boolean>
}
