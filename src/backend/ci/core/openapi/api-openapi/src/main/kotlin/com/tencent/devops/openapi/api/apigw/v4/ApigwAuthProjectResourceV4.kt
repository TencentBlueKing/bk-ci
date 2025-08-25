package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.openapi.BkApigwApi
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

@Tag(name = "OPENAPI_AUTH_V4", description = "OPENAPI-权限相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/project/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthProjectResourceV4 {
    @GET
    @Path("/get_project_permission_info")
    @Operation(
        summary = "获取项目权限信息",
        tags = ["v4_app_get_project_permission_info"]
    )
    fun getProjectPermissionInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<ProjectPermissionInfoVO>

    @GET
    @Path("/getResourceGroupUsers")
    @Operation(
        summary = """获取项目权限分组成员
           该接口是一个可以查多种权限名单的接口，这取决于resourceType。
           示例①：查询A项目下p-B流水线拥有者有哪些，如果group为null，则会取有p-B流水线相关权限的所有人。
               - projectId: A
               - resourceType: PIPELINE_DEFAULT
               - resourceCode: p-B
               - group: RESOURCE_MANAGER
           示例②：查询A项目管理员有哪些,如果group为null，则A项目下所有人。
               - projectId: A
               - resourceType: PROJECT
               - resourceCode: A
               - group: MANAGER
        """,
        tags = ["v4_app_get_project_permission_members"]
    )
    fun getResourceGroupUsers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @PathParam("projectId")
        @Parameter(description = "项目Code", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: AuthResourceType,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String,
        @QueryParam("group")
        @Parameter(description = "资源用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/get_project_group_and_users")
    @Operation(summary = "获取项目组成员", tags = ["v4_app_get_project_group_and_users"])
    fun getProjectGroupAndUserList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<BkAuthGroupAndUserList>>

    @GET
    @Path("/{groupId}/get_group_permission_detail")
    @Operation(summary = "查询用户组权限详情", tags = ["v4_app_get_group_permission_detail"])
    fun getGroupPermissionDetail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户组ID")
        @PathParam("groupId")
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>>

    @POST
    @Path("/batch_add_resource_group_members")
    @Operation(summary = "用户组批量添加成员", tags = ["v4_app_batch_add_resource_group_members"])
    fun batchAddResourceGroupMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @DELETE
    @Path("/batch_delete_resource_group_members")
    @Operation(summary = "用户组批量删除成员", tags = ["v4_app_batch_delete_resource_group_members"])
    fun batchDeleteResourceGroupMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "删除信息", required = true)
        deleteInfo: ProjectDeleteUserInfo
    ): Result<Boolean>

    @POST
    @Path("/create_group_by_group_code/{resourceType}")
    @Operation(summary = "根据groupCode添加用户组", tags = ["v4_app_create_group_by_group_code"])
    fun createGroupByGroupCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组code,CI管理员为CI_MANAGER", required = true)
        @QueryParam("groupCode")
        groupCode: BkAuthGroup,
        @Parameter(description = "用户组名称", required = true)
        @QueryParam("groupName")
        groupName: String?,
        @Parameter(description = "用户组描述", required = true)
        @QueryParam("groupDesc")
        groupDesc: String?
    ): Result<Int>

    @POST
    @Path("/create_custom_group_and_permissions/")
    @Operation(summary = "创建自定义用户和权限", tags = ["v4_app_create_custom_group_and_permissions"])
    fun createCustomGroupAndPermissions(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "自定义组创建请求体", required = true)
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int>

    @POST
    @Path("/create_group")
    @Operation(summary = "创建自定义组(不包含权限，空权限组)", tags = ["v4_app_create_group"])
    fun createGroup(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "添加用户组实体", required = true)
        groupAddDTO: GroupAddDTO
    ): Result<Int>

    @DELETE
    @Path("/delete_group/{resourceType}")
    @Operation(summary = "刪除用户组", tags = ["v4_app_delete_group"])
    fun deleteGroup(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组ID", required = true)
        @QueryParam("groupId")
        groupId: Int
    ): Result<Boolean>
}
