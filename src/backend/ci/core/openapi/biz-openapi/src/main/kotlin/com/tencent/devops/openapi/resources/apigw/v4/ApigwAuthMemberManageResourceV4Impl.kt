package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceAuthAiResource
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
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
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthMemberManageResourceV4
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("TooManyFunctions", "LongParameterList")
class ApigwAuthMemberManageResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwAuthMemberManageResourceV4 {

    override fun listGroups(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        condition: IamGroupIdsQueryConditionDTO
    ): Result<SQLPage<AuthResourceGroup>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|listGroups|$projectId")
        return client.get(ServiceAuthAiResource::class).listGroups(
            userId = userId,
            projectId = projectId,
            condition = condition.copy(projectCode = projectId)
        )
    }

    override fun getGroupPermissionDetail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        groupId: Int
    ): Result<List<ResourceGroupPermissionDTO>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|getGroupPermissionDetail|$projectId|$groupId")
        return client.get(ServiceAuthAiResource::class).getGroupPermissionDetail(
            userId = userId,
            projectId = projectId,
            groupId = groupId
        )
    }

    override fun listGroupMembers(
        appCode: String?,
        apigwType: String?,
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
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|listGroupMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).listGroupMembers(
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
            pageSize = pageSize
        )
    }

    override fun listProjectMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        memberType: String?,
        userName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|listProjectMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).listProjectMembers(
            userId = userId,
            projectId = projectId,
            memberType = memberType,
            userName = userName,
            departedFlag = departedFlag,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getMemberGroupCount(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): Result<List<ResourceType2CountVo>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|getMemberGroupCount|$projectId|$memberId")
        return client.get(ServiceAuthAiResource::class).getMemberGroupCount(
            userId = userId,
            projectId = projectId,
            memberId = memberId,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode
        )
    }

    override fun getMemberGroupsDetails(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<GroupDetailsInfoVo>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|getMemberGroupsDetails|$projectId|$memberId")
        return client.get(ServiceAuthAiResource::class).getMemberGroupsDetails(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            memberId = memberId,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getAllMemberGroupsDetails(
        appCode: String?,
        apigwType: String?,
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
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|getAllMemberGroupsDetails|$projectId|$memberId")
        return client.get(ServiceAuthAiResource::class).getAllMemberGroupsDetails(
            userId = userId,
            projectId = projectId,
            memberId = memberId,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            groupName = groupName,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode,
            action = action,
            page = page,
            pageSize = pageSize
        )
    }

    override fun addGroupMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|addGroupMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).addGroupMembers(
            userId = userId,
            projectId = projectId,
            createInfo = createInfo
        )
    }

    override fun batchRenewalMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchRenewalMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).batchRenewalMembers(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun applyRenewalGroupMember(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        groupIds: String,
        renewalDays: Int,
        reason: String
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|applyRenewalGroupMember|$projectId")
        return client.get(ServiceAuthAiResource::class).applyRenewalGroupMember(
            userId = userId,
            projectId = projectId,
            groupIds = groupIds,
            renewalDays = renewalDays,
            reason = reason
        )
    }

    override fun batchRemoveMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchRemoveMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).batchRemoveMembers(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun batchHandoverMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchHandoverMembers|$projectId")
        return client.get(ServiceAuthAiResource::class).batchHandoverMembers(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun exitGroupsFromPersonal(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<String> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|exitGroupsFromPersonal|$projectId")
        return client.get(ServiceAuthAiResource::class).exitGroupsFromPersonal(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun applyHandoverFromPersonal(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<String> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|applyHandoverFromPersonal|$projectId")
        return client.get(ServiceAuthAiResource::class).applyHandoverFromPersonal(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun batchOperateCheck(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchOperateCheck|$projectId|$batchOperateType")
        return client.get(ServiceAuthAiResource::class).batchOperateCheck(
            userId = userId,
            projectId = projectId,
            batchOperateType = batchOperateType,
            request = request
        )
    }

    override fun removeMemberFromProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|removeMemberFromProject|$projectId")
        return client.get(ServiceAuthAiResource::class).removeMemberFromProject(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun applyToJoinGroup(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|applyToJoinGroup|$projectId")
        return client.get(ServiceAuthAiResource::class).applyToJoinGroup(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun checkMemberExitsProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<MemberExitsProjectCheckVo> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|checkMemberExitsProject|$projectId")
        return client.get(ServiceAuthAiResource::class).checkMemberExitsProject(
            userId = userId,
            projectId = projectId
        )
    }

    override fun memberExitsProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: AiMemberExitsProjectReq
    ): Result<String> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|memberExitsProject|$projectId")
        return client.get(ServiceAuthAiResource::class).memberExitsProject(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun batchRemoveMemberFromProjectCheck(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        targetMemberIds: List<String>
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchRemoveMemberFromProjectCheck|$projectId")
        return client.get(ServiceAuthAiResource::class).batchRemoveMemberFromProjectCheck(
            userId = userId,
            projectId = projectId,
            targetMemberIds = targetMemberIds
        )
    }

    override fun batchRemoveMemberFromProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: AiBatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|batchRemoveMemberFromProject|$projectId")
        return client.get(ServiceAuthAiResource::class).batchRemoveMemberFromProject(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun searchUsers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        keyword: String,
        queryProjectId: String?,
        limit: Int
    ): Result<UserSearchResultVO> {
        logger.info("OPENAPI_AUTH_MEMBER_MANAGE_V4|$appCode|$userId|searchUsers|$queryProjectId")
        return client.get(ServiceAuthAiResource::class).searchUsers(
            userId = userId,
            keyword = keyword,
            projectId = queryProjectId,
            limit = limit
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwAuthMemberManageResourceV4Impl::class.java)
    }
}
