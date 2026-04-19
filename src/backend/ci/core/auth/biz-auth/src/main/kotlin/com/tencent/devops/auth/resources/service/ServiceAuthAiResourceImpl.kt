package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceAuthAiResource
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
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
import com.tencent.devops.auth.pojo.vo.ResolvedUserByNameVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.service.AuthAiService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkManagerCheck
import com.tencent.devops.common.auth.api.BkProjectMemberCheck
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import java.util.concurrent.TimeUnit

@RestResource
@Suppress("TooManyFunctions", "LongParameterList")
class ServiceAuthAiResourceImpl(
    private val rbacCommonService: RbacCommonService,
    private val permissionResourceService: PermissionResourceService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val authAiService: AuthAiService
) : ServiceAuthAiResource {

    override fun listResourceTypes(userId: String): Result<List<ResourceTypeInfoVo>> {
        return Result(rbacCommonService.listResourceTypes())
    }

    override fun listActions(userId: String, resourceType: String): Result<List<ActionInfoVo>> {
        return Result(rbacCommonService.listResourceType2Action(resourceType))
    }

    @BkProjectMemberCheck
    override fun searchResource(
        userId: String,
        projectId: String,
        resourceType: String,
        keyword: String
    ): Result<List<AuthResourceInfo>> {
        return Result(authAiService.searchResource(userId, projectId, resourceType, keyword))
    }

    @BkProjectMemberCheck
    override fun getResourceByName(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceName: String
    ): Result<AuthResourceInfo?> {
        return Result(
            permissionResourceService.getResourceByName(
                projectCode = projectId,
                resourceType = resourceType,
                resourceName = resourceName
            )
        )
    }

    @BkProjectMemberCheck
    override fun getResourceByCode(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<AuthResourceInfo?> {
        return Result(
            permissionResourceService.getResourceByCode(
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    @BkProjectMemberCheck
    override fun listGroups(
        userId: String,
        projectId: String,
        condition: IamGroupIdsQueryConditionDTO
    ): Result<SQLPage<AuthResourceGroup>> {
        return Result(authAiService.listAuthResourceGroups(userId, projectId, condition))
    }

    @BkProjectMemberCheck
    override fun getGroupPermissionDetail(
        userId: String,
        projectId: String,
        groupId: Int
    ): Result<List<ResourceGroupPermissionDTO>> {
        return Result(authAiService.getGroupPermissionDetail(userId, projectId, groupId))
    }

    @BkManagerCheck
    override fun listGroupMembers(
        userId: String,
        projectId: String,
        resourceType: String?,
        resourceCode: String?,
        iamGroupId: Int?,
        groupCode: String?,
        memberId: String?,
        memberType: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<AuthResourceGroupMember>> {
        return Result(
            authAiService.listGroupMembers(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                iamGroupId = iamGroupId,
                groupCode = groupCode,
                memberId = memberId,
                memberType = memberType,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                page = page,
                pageSize = pageSize.coerceIn(1, 100)
            )
        )
    }

    @BkManagerCheck
    override fun listProjectMembers(
        userId: String,
        projectId: String,
        memberType: String?,
        userName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>> {
        return Result(
            permissionResourceMemberService.listProjectMembers(
                projectCode = projectId,
                memberType = memberType,
                userName = userName,
                deptName = null,
                departedFlag = departedFlag,
                page = page,
                pageSize = pageSize
            )
        )
    }

    @BkProjectMemberCheck
    override fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): Result<List<ResourceType2CountVo>> {
        return Result(
            authAiService.getMemberGroupCount(
                userId, projectId, memberId, relatedResourceType, relatedResourceCode
            )
        )
    }

    @BkProjectMemberCheck
    override fun getMemberGroupsDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<GroupDetailsInfoVo>> {
        return Result(
            authAiService.getMemberGroupsDetails(
                userId, projectId, resourceType, memberId,
                relatedResourceType, relatedResourceCode, page, pageSize
            )
        )
    }

    @BkProjectMemberCheck
    override fun getAllMemberGroupsDetails(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: String?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<GroupDetailsInfoVo>> {
        return Result(
            authAiService.getAllMemberGroupsDetails(
                userId, projectId, memberId, resourceType, iamGroupIds, groupName,
                minExpiredAt, maxExpiredAt, relatedResourceType, relatedResourceCode,
                action, page, pageSize
            )
        )
    }

    @BkManagerCheck
    override fun addGroupMembers(
        userId: String,
        projectId: String,
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        val now = System.currentTimeMillis() / 1000
        val expiredTime = now + TimeUnit.DAYS.toSeconds(
            createInfo.expiredTime ?: DEFAULT_EXPIRED_DAYS
        )
        return Result(
            permissionResourceMemberService.batchAddResourceGroupMembers(
                projectCode = projectId,
                iamGroupId = createInfo.groupId!!,
                expiredTime = expiredTime,
                members = createInfo.userIds,
                departments = createInfo.deptIds
            )
        )
    }

    @BkManagerCheck
    override fun deleteGroupMembers(
        userId: String,
        projectId: String,
        deleteInfo: ProjectDeleteUserInfo
    ): Result<Boolean> {
        return Result(
            permissionResourceMemberService.batchDeleteResourceGroupMembers(
                projectCode = projectId,
                iamGroupId = deleteInfo.groupId!!,
                members = deleteInfo.userIds,
                departments = deleteInfo.deptIds
            )
        )
    }

    @BkManagerCheck
    override fun batchRenewalMembers(
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Result<Boolean> {
        return Result(authAiService.batchRenewalMembers(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun applyRenewalGroupMember(
        userId: String,
        projectId: String,
        groupIds: String,
        renewalDays: Int,
        reason: String
    ): Result<Boolean> {
        return Result(authAiService.applyRenewalGroupMember(userId, projectId, groupIds, renewalDays, reason))
    }

    @BkProjectMemberCheck
    override fun batchRemoveMembers(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<Boolean> {
        return Result(authAiService.batchRemoveMembers(userId, projectId, request))
    }

    @BkManagerCheck
    override fun batchHandoverMembers(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<Boolean> {
        return Result(authAiService.batchHandoverMembers(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun exitGroupsFromPersonal(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<String> {
        return Result(authAiService.exitGroupsFromPersonal(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun applyHandoverFromPersonal(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<String> {
        return Result(authAiService.applyHandoverFromPersonal(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun batchOperateCheck(
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo> {
        return Result(authAiService.batchOperateCheck(userId, projectId, batchOperateType, request))
    }

    @BkProjectMemberCheck
    override fun removeMemberFromProject(
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>> {
        return Result(authAiService.removeMemberFromProject(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun analyzeUserPermissions(
        userId: String,
        projectId: String,
        memberId: String
    ): Result<UserPermissionAnalysisVO> {
        return Result(authAiService.analyzeUserPermissions(userId, projectId, memberId))
    }

    @BkProjectMemberCheck
    override fun getResourcePermissionsMatrix(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<ResourcePermissionsMatrixVO> {
        return Result(authAiService.getResourcePermissionsMatrix(userId, projectId, resourceType, resourceCode))
    }

    @BkProjectMemberCheck
    override fun recommendGroupsForGrant(
        userId: String,
        projectId: String,
        request: GroupRecommendReq
    ): Result<GroupRecommendationVO> {
        return Result(authAiService.recommendGroupsForGrant(userId, projectId, request))
    }

    override fun applyToJoinGroup(
        userId: String,
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Result<Boolean> {
        return Result(authAiService.applyToJoinGroup(userId, projectId, request))
    }

    @BkProjectMemberCheck
    override fun checkMemberExitsProject(
        userId: String,
        projectId: String
    ): Result<MemberExitsProjectCheckVo> {
        return Result(
            permissionManageFacadeService.checkMemberExitsProject(
                projectCode = projectId,
                userId = userId
            )
        )
    }

    @BkProjectMemberCheck
    override fun memberExitsProject(
        userId: String,
        projectId: String,
        request: AiMemberExitsProjectReq
    ): Result<String> {
        return Result(
            permissionManageFacadeService.memberExitsProject(
                projectCode = projectId,
                request = RemoveMemberFromProjectReq(
                    targetMember = ResourceMemberInfo(id = userId, type = USER_TYPE),
                    handoverTo = request.handoverToMemberId?.let {
                        ResourceMemberInfo(id = it, type = USER_TYPE)
                    }
                )
            )
        )
    }

    @BkManagerCheck
    override fun batchRemoveMemberFromProjectCheck(
        userId: String,
        projectId: String,
        targetMemberIds: List<String>
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchRemoveMemberFromProjectCheck(
                userId = userId,
                projectCode = projectId,
                targetMembers = targetMemberIds.map { ResourceMemberInfo(id = it, type = USER_TYPE) }
            )
        )
    }

    @BkManagerCheck
    override fun batchRemoveMemberFromProject(
        userId: String,
        projectId: String,
        request: AiBatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse> {
        return Result(
            permissionManageFacadeService.batchRemoveMemberFromProject(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = BatchRemoveMemberFromProjectReq(
                    targetMembers = request.targetMemberIds.map {
                        ResourceMemberInfo(id = it, type = USER_TYPE)
                    },
                    handoverTo = request.handoverToMemberId?.let {
                        ResourceMemberInfo(id = it, type = USER_TYPE)
                    }
                )
            )
        )
    }

    @BkProjectMemberCheck
    override fun diagnosePermission(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): Result<PermissionDiagnoseVO> {
        return Result(
            authAiService.diagnosePermission(
                userId = userId,
                projectId = projectId,
                memberId = memberId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
        )
    }

    @BkManagerCheck
    override fun clonePermissions(
        userId: String,
        projectId: String,
        sourceUserId: String,
        targetUserId: String,
        resourceTypes: String?,
        dryRun: Boolean
    ): Result<PermissionCloneResultVO> {
        val resourceTypeList = resourceTypes?.split(",")?.filter { it.isNotBlank() }
        return Result(
            authAiService.clonePermissions(
                userId = userId,
                projectId = projectId,
                sourceUserId = sourceUserId,
                targetUserId = targetUserId,
                resourceTypes = resourceTypeList,
                dryRun = dryRun
            )
        )
    }

    @BkManagerCheck
    override fun comparePermissions(
        userId: String,
        projectId: String,
        userIdA: String,
        userIdB: String,
        resourceType: String?
    ): Result<PermissionCompareVO> {
        return Result(
            authAiService.comparePermissions(
                userId = userId,
                projectId = projectId,
                userIdA = userIdA,
                userIdB = userIdB,
                resourceType = resourceType
            )
        )
    }

    @BkManagerCheck
    override fun checkAuthorizationHealth(
        userId: String,
        projectId: String
    ): Result<AuthorizationHealthVO> {
        return Result(authAiService.checkAuthorizationHealth(userId, projectId))
    }

    @BkProjectMemberCheck
    override fun searchUsers(
        userId: String,
        keyword: String,
        projectId: String?,
        limit: Int
    ): Result<UserSearchResultVO> {
        return Result(authAiService.searchUsers(userId, keyword, projectId, limit))
    }

    override fun resolveUsersByName(
        userName: String
    ): Result<List<ResolvedUserByNameVO>> {
        return Result(authAiService.resolveUsersByName(userName))
    }

    @BkProjectMemberCheck
    override fun checkMemberExitWithRecommendation(
        userId: String,
        projectId: String,
        targetMemberId: String,
        handoverTo: String?,
        groupIds: String?,
        recommendLimit: Int
    ): Result<MemberExitCheckVO> {
        return Result(
            authAiService.checkMemberExitWithRecommendation(
                userId = userId,
                projectId = projectId,
                targetMemberId = targetMemberId,
                handoverTo = handoverTo,
                groupIds = groupIds,
                recommendLimit = recommendLimit
            )
        )
    }

    companion object {
        private const val USER_TYPE = "user"
        private const val DEFAULT_EXPIRED_DAYS = 365L
    }
}
