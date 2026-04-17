package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiBatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.AiMemberExitsProjectReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthorizationHealthVO
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.GroupRecommendationVO
import com.tencent.devops.auth.pojo.vo.MemberExitCheckVO
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.PermissionCloneResultVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareVO
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.auth.pojo.vo.ResolvedUserByNameVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
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
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(
    name = "SERVICE_AUTH_AI",
    description = "AI-权限管理接口"
)
@Path("/service/auth/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthAiResource {

    @GET
    @Path("/resource/types")
    @Operation(summary = "获取资源类型列表")
    fun listResourceTypes(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("/resource/actions")
    @Operation(summary = "获取操作类型列表")
    fun listActions(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String
    ): Result<List<ActionInfoVo>>

    @GET
    @Path("/projects/{projectId}/resource/search")
    @Operation(
        summary = "根据资源名称或Code搜索资源",
        description = "优先按Code精确匹配，" +
            "再按名称精确匹配，最后模糊匹配"
    )
    fun searchResource(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(
            description = "资源类型（如 pipeline、credential 等）",
            required = true
        )
        resourceType: String,
        @QueryParam("keyword")
        @Parameter(
            description = "资源名称或Code",
            required = true
        )
        keyword: String
    ): Result<List<AuthResourceInfo>>

    @GET
    @Path("/projects/{projectId}/resource/byName")
    @Operation(summary = "根据名称查询资源")
    fun getResourceByName(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("resourceName")
        @Parameter(description = "资源名称", required = true)
        resourceName: String
    ): Result<AuthResourceInfo?>

    @GET
    @Path("/projects/{projectId}/resource/byCode")
    @Operation(summary = "根据Code查询资源")
    fun getResourceByCode(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源Code", required = true)
        resourceCode: String
    ): Result<AuthResourceInfo?>

    @POST
    @Path("/projects/{projectId}/groups")
    @Operation(summary = "查询用户组列表（支持分页，见查询条件中的 page / pageSize）")
    fun listGroups(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "查询条件", required = true)
        condition: IamGroupIdsQueryConditionDTO
    ): Result<SQLPage<AuthResourceGroup>>

    @GET
    @Path("/projects/{projectId}/groups/{groupId}/permissions")
    @Operation(summary = "查询用户组权限详情")
    fun getGroupPermissionDetail(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("groupId")
        @Parameter(description = "用户组ID", required = true)
        groupId: Int
    ): Result<List<ResourceGroupPermissionDTO>>

    @GET
    @Path("/projects/{projectId}/groups/members")
    @Operation(
        summary = "查询用户组成员详情列表",
        description = "支持丰富的筛选条件，返回成员详细信息（含过期时间、成员类型等）"
    )
    fun listGroupMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型过滤，如 project、pipeline")
        resourceType: String? = null,
        @QueryParam("resourceCode")
        @Parameter(description = "资源Code过滤")
        resourceCode: String? = null,
        @QueryParam("iamGroupId")
        @Parameter(description = "用户组ID（IAM组ID）过滤")
        iamGroupId: Int? = null,
        @QueryParam("groupCode")
        @Parameter(description = "用户组类型过滤，如 manager/developer/viewer")
        groupCode: String? = null,
        @QueryParam("memberId")
        @Parameter(description = "成员ID过滤")
        memberId: String? = null,
        @QueryParam("memberType")
        @Parameter(description = "成员类型过滤：user/department/template")
        memberType: String? = null,
        @QueryParam("minExpiredAt")
        @Parameter(description = "最小过期时间戳（毫秒），用于查询即将过期或未过期的成员")
        minExpiredAt: Long? = null,
        @QueryParam("maxExpiredAt")
        @Parameter(description = "最大过期时间戳（毫秒），用于查询已过期的成员")
        maxExpiredAt: Long? = null,
        @QueryParam("page")
        @Parameter(description = "页码，默认1")
        page: Int = 1,
        @QueryParam("pageSize")
        @Parameter(description = "每页条数，默认20，最大100")
        pageSize: Int = 20
    ): Result<SQLPage<AuthResourceGroupMember>>

    @GET
    @Path("/projects/{projectId}/members")
    @Operation(summary = "获取项目全体成员")
    fun listProjectMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("memberType")
        @Parameter(description = "成员类型(user/department)")
        memberType: String? = null,
        @QueryParam("userName")
        @Parameter(description = "用户名搜索")
        userName: String? = null,
        @QueryParam("page")
        @Parameter(description = "页码")
        page: Int = 1,
        @QueryParam("pageSize")
        @Parameter(description = "每页条数")
        pageSize: Int = 20
    ): Result<SQLPage<ResourceMemberInfo>>

    @GET
    @Path("/projects/{projectId}/members/groupCount")
    @Operation(summary = "获取成员用户组数量")
    fun getMemberGroupCount(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("memberId")
        @Parameter(description = "成员ID", required = true)
        memberId: String,
        @QueryParam("relatedResourceType")
        @Parameter(description = "关联资源类型")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "关联资源Code")
        relatedResourceCode: String? = null
    ): Result<List<ResourceType2CountVo>>

    @GET
    @Path(
        "/projects/{projectId}/members/{resourceType}/groups"
    )
    @Operation(summary = "获取成员用户组详情")
    fun getMemberGroupsDetails(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("memberId")
        @Parameter(description = "成员ID", required = true)
        memberId: String,
        @QueryParam("relatedResourceType")
        @Parameter(description = "关联资源类型过滤")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "关联资源Code过滤")
        relatedResourceCode: String? = null,
        @QueryParam("page")
        @Parameter(description = "页码")
        page: Int = 1,
        @QueryParam("pageSize")
        @Parameter(description = "每页条数")
        pageSize: Int = 20
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @GET
    @Path("/projects/{projectId}/members/allGroups")
    @Operation(
        summary = "获取成员所有用户组详情",
        description = "一次性查询成员在所有资源类型下的用户组详情，支持多种过滤条件"
    )
    fun getAllMemberGroupsDetails(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("memberId")
        @Parameter(description = "成员ID", required = true)
        memberId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型过滤，不传则查询所有资源类型")
        resourceType: String? = null,
        @QueryParam("iamGroupIds")
        @Parameter(description = "IAM用户组ID列表，逗号分隔")
        iamGroupIds: String? = null,
        @QueryParam("groupName")
        @Parameter(description = "用户组名称搜索")
        groupName: String? = null,
        @QueryParam("minExpiredAt")
        @Parameter(description = "最小过期时间戳（毫秒），用于查询即将过期的权限")
        minExpiredAt: Long? = null,
        @QueryParam("maxExpiredAt")
        @Parameter(description = "最大过期时间戳（毫秒），用于查询即将过期的权限")
        maxExpiredAt: Long? = null,
        @QueryParam("relatedResourceType")
        @Parameter(description = "关联资源类型过滤")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "关联资源Code过滤")
        relatedResourceCode: String? = null,
        @QueryParam("action")
        @Parameter(description = "操作权限过滤，如 pipeline_execute")
        action: String? = null,
        @QueryParam("page")
        @Parameter(description = "页码")
        page: Int = 1,
        @QueryParam("pageSize")
        @Parameter(description = "每页条数")
        pageSize: Int = 500
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @POST
    @Path("/projects/{projectId}/members/add")
    @Operation(summary = "批量添加用户组成员")
    fun addGroupMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "添加成员信息", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @DELETE
    @Path("/projects/{projectId}/members/delete")
    @Operation(summary = "批量删除用户组成员")
    fun deleteGroupMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "删除成员信息", required = true)
        deleteInfo: ProjectDeleteUserInfo
    ): Result<Boolean>

    @PUT
    @Path("/projects/{projectId}/members/renewal")
    @Operation(summary = "批量续期用户组成员")
    fun batchRenewalMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "续期请求体", required = true)
        request: BatchRenewalMembersReq
    ): Result<Boolean>

    @POST
    @Path("/projects/{projectId}/members/apply-renewal")
    @Operation(
        summary = "普通用户申请续期权限",
        description = "用户申请续期自己在用户组中的权限，需要审批"
    )
    fun applyRenewalGroupMember(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("groupIds")
        @Parameter(description = "用户组ID列表，逗号分隔", required = true)
        groupIds: String,
        @QueryParam("renewalDays")
        @Parameter(description = "续期天数", required = true)
        renewalDays: Int,
        @QueryParam("reason")
        @Parameter(description = "申请理由", required = true)
        reason: String
    ): Result<Boolean>

    @DELETE
    @Path("/projects/{projectId}/members/remove")
    @Operation(summary = "批量移除用户组成员")
    fun batchRemoveMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "移除请求体", required = true)
        request: BatchRemoveMembersReq
    ): Result<Boolean>

    @PUT
    @Path("/projects/{projectId}/members/handover")
    @Operation(summary = "批量交接用户组成员")
    fun batchHandoverMembers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "交接请求体", required = true)
        request: BatchHandoverMembersReq
    ): Result<Boolean>

    @DELETE
    @Path("/projects/{projectId}/members/exitGroups")
    @Operation(
        summary = "成员自助退出用户组",
        description = "当前用户退出指定用户组（个人视角，无需管理员权限）"
    )
    fun exitGroupsFromPersonal(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "退出请求体", required = true)
        request: BatchRemoveMembersReq
    ): Result<String>

    @POST
    @Path("/projects/{projectId}/members/applyHandover")
    @Operation(
        summary = "成员自助申请交接用户组",
        description = "当前用户申请将指定用户组权限交接给他人（需审批）"
    )
    fun applyHandoverFromPersonal(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "交接申请请求体", required = true)
        request: BatchHandoverMembersReq
    ): Result<String>

    @POST
    @Path(
        "/projects/{projectId}/members/{batchOperateType}/check"
    )
    @Operation(summary = "批量操作成员检查")
    fun batchOperateCheck(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("batchOperateType")
        @Parameter(
            description = "操作类型(RENEWAL/REMOVE/HANDOVER)",
            required = true
        )
        batchOperateType: BatchOperateType,
        @Parameter(description = "检查请求体", required = true)
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/projects/{projectId}/members/removeFromProject")
    @Operation(summary = "将用户移出项目")
    fun removeMemberFromProject(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "移出项目请求体", required = true)
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @GET
    @Path("/projects/{projectId}/members/{memberId}/analysis")
    @Operation(summary = "用户权限分析报告")
    fun analyzeUserPermissions(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "操作人用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("memberId")
        @Parameter(description = "目标成员ID", required = true)
        memberId: String
    ): Result<UserPermissionAnalysisVO>

    @GET
    @Path(
        "/projects/{projectId}/resources" +
            "/{resourceType}/{resourceCode}/permissions-matrix"
    )
    @Operation(summary = "资源权限矩阵")
    fun getResourcePermissionsMatrix(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "操作人用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @PathParam("resourceCode")
        @Parameter(description = "资源Code", required = true)
        resourceCode: String
    ): Result<ResourcePermissionsMatrixVO>

    @POST
    @Path("/projects/{projectId}/groups/recommend")
    @Operation(summary = "智能推荐用户组")
    fun recommendGroupsForGrant(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "操作人用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "推荐请求体", required = true)
        request: GroupRecommendReq
    ): Result<GroupRecommendationVO>

    @POST
    @Path("/projects/{projectId}/apply")
    @Operation(summary = "申请加入用户组")
    fun applyToJoinGroup(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "申请人用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "申请请求体", required = true)
        request: AiApplyJoinGroupReq
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectId}/members/exitsProjectCheck")
    @Operation(summary = "用户主动退出项目检查")
    fun checkMemberExitsProject(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<MemberExitsProjectCheckVo>

    @PUT
    @Path("/projects/{projectId}/members/exitsProject")
    @Operation(summary = "用户主动退出项目")
    fun memberExitsProject(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "退出项目请求体", required = true)
        request: AiMemberExitsProjectReq
    ): Result<String>

    @POST
    @Path("/projects/{projectId}/members/batchRemoveFromProjectCheck")
    @Operation(summary = "批量将用户移出项目检查")
    fun batchRemoveMemberFromProjectCheck(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "目标成员ID列表", required = true)
        targetMemberIds: List<String>
    ): Result<Boolean>

    @PUT
    @Path("/projects/{projectId}/members/batchRemoveFromProject")
    @Operation(summary = "批量将用户移出项目")
    fun batchRemoveMemberFromProject(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "批量移出项目请求体", required = true)
        request: AiBatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse>

    @GET
    @Path("/projects/{projectId}/diagnose")
    @Operation(summary = "权限诊断 - 分析用户为什么没有某个权限")
    fun diagnosePermission(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("memberId")
        @Parameter(description = "目标成员ID", required = true)
        memberId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源Code", required = true)
        resourceCode: String,
        @QueryParam("action")
        @Parameter(description = "操作类型", required = true)
        action: String
    ): Result<PermissionDiagnoseVO>

    @POST
    @Path("/projects/{projectId}/clone")
    @Operation(summary = "权限克隆 - 将一个用户的权限复制给另一个用户")
    fun clonePermissions(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("sourceUserId")
        @Parameter(description = "来源用户ID", required = true)
        sourceUserId: String,
        @QueryParam("targetUserId")
        @Parameter(description = "目标用户ID", required = true)
        targetUserId: String,
        @QueryParam("resourceTypes")
        @Parameter(description = "限定的资源类型列表，逗号分隔")
        resourceTypes: String? = null,
        @QueryParam("dryRun")
        @Parameter(description = "是否预检查模式")
        dryRun: Boolean = true
    ): Result<PermissionCloneResultVO>

    @GET
    @Path("/projects/{projectId}/compare")
    @Operation(summary = "权限对比 - 比较两个用户的权限差异")
    fun comparePermissions(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @QueryParam("userIdA")
        @Parameter(description = "用户A的ID", required = true)
        userIdA: String,
        @QueryParam("userIdB")
        @Parameter(description = "用户B的ID", required = true)
        userIdB: String,
        @QueryParam("resourceType")
        @Parameter(description = "限定的资源类型")
        resourceType: String? = null
    ): Result<PermissionCompareVO>

    @GET
    @Path("/projects/{projectId}/authorization/health")
    @Operation(summary = "授权健康检查 - 扫描项目授权风险")
    fun checkAuthorizationHealth(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<AuthorizationHealthVO>

    @GET
    @Path("/users/search")
    @Operation(summary = "用户搜索 - 根据关键词搜索用户")
    fun searchUsers(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @QueryParam("keyword")
        @Parameter(description = "搜索关键词", required = true)
        keyword: String,
        @QueryParam("projectId")
        @Parameter(description = "项目ID，限定在项目成员中搜索")
        projectId: String? = null,
        @QueryParam("limit")
        @Parameter(description = "返回数量限制")
        limit: Int = 10
    ): Result<UserSearchResultVO>

    @GET
    @Path("/users/resolve")
    @Operation(summary = "用户名称解析 - 根据中文名称查询用户列表（精确匹配，可重名）")
    fun resolveUsersByName(
        @QueryParam("userName")
        @Parameter(description = "用户中文名称", required = true)
        userName: String
    ): Result<List<ResolvedUserByNameVO>>

    @GET
    @Path("/projects/{projectId}/members/{targetMemberId}/exit/check")
    @Operation(summary = "检查成员退出/交接权限 - 综合检查并返回推荐交接人")
    fun checkMemberExitWithRecommendation(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @PathParam("targetMemberId")
        @Parameter(description = "目标成员ID", required = true)
        targetMemberId: String,
        @QueryParam("handoverTo")
        @Parameter(description = "指定的交接人ID")
        handoverTo: String? = null,
        @QueryParam("groupIds")
        @Parameter(description = "用户组ID列表，逗号分隔（不传则检查退出整个项目）")
        groupIds: String? = null,
        @QueryParam("recommendLimit")
        @Parameter(description = "推荐候选人数量限制")
        recommendLimit: Int = 5
    ): Result<MemberExitCheckVO>
}
