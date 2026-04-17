package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthMemberResourceV4
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthMemberResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwAuthMemberResourceV4 {

    companion object {
        private val logger =
            LoggerFactory.getLogger(
                ApigwAuthMemberResourceV4Impl::class.java
            )
        private const val USER_TYPE = "user"
    }

    override fun batchRenewalMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " batchRenewalMembers" +
                "|$appCode|$userId|$projectId"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .batchRenewalGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                renewalConditionReq = GroupMemberRenewalConditionReq(
                    groupIds = request.groupIds.map {
                        MemberGroupJoinedDTO(
                            id = it,
                            memberType = MemberType.USER
                        )
                    },
                    targetMember = ResourceMemberInfo(
                        id = request.targetMemberId,
                        type = USER_TYPE
                    ),
                    operateChannel = OperateChannel.MANAGER,
                    renewalDuration = request.renewalDuration
                )
            )
    }

    override fun batchRemoveMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " batchRemoveMembers" +
                "|$appCode|$userId|$projectId"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .batchRemoveGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                removeMemberDTO = GroupMemberRemoveConditionReq(
                    groupIds = request.groupIds.map {
                        MemberGroupJoinedDTO(
                            id = it,
                            memberType = MemberType.USER
                        )
                    },
                    targetMember = ResourceMemberInfo(
                        id = request.targetMemberId,
                        type = USER_TYPE
                    ),
                    operateChannel = OperateChannel.MANAGER,
                    handoverTo = request.handoverToMemberId?.let {
                        ResourceMemberInfo(id = it, type = USER_TYPE)
                    }
                )
            )
    }

    override fun batchOperateMembersCheck(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " batchOperateMembersCheck" +
                "|$appCode|$userId|$projectId|$batchOperateType"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .batchOperateGroupMembersCheck(
                userId = userId,
                projectCode = projectId,
                batchOperateType = batchOperateType,
                conditionReq = GroupMemberCommonConditionReq(
                    groupIds = request.groupIds.map {
                        MemberGroupJoinedDTO(
                            id = it,
                            memberType = MemberType.USER
                        )
                    },
                    targetMember = ResourceMemberInfo(
                        id = request.targetMemberId,
                        type = USER_TYPE
                    ),
                    operateChannel = OperateChannel.MANAGER
                )
            )
    }

    override fun batchHandoverMembers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " batchHandoverMembers" +
                "|$appCode|$userId|$projectId"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .batchHandoverGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                handoverMemberDTO = GroupMemberHandoverConditionReq(
                    groupIds = request.groupIds.map {
                        MemberGroupJoinedDTO(
                            id = it,
                            memberType = MemberType.USER
                        )
                    },
                    targetMember = ResourceMemberInfo(
                        id = request.targetMemberId,
                        type = USER_TYPE
                    ),
                    operateChannel = OperateChannel.MANAGER,
                    handoverTo = ResourceMemberInfo(
                        id = request.handoverToMemberId,
                        type = USER_TYPE
                    )
                )
            )
    }

    override fun removeMemberFromProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " removeMemberFromProject" +
                "|$appCode|$userId|$projectId"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .removeMemberFromProject(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = RemoveMemberFromProjectReq(
                    targetMember = ResourceMemberInfo(
                        id = request.targetMemberId,
                        type = USER_TYPE
                    ),
                    handoverTo = request.handoverToMemberId?.let {
                        ResourceMemberInfo(id = it, type = USER_TYPE)
                    }
                )
            )
    }

    override fun removeMemberFromProjectCheck(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        targetMemberId: String
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_MEMBER_V4" +
                " removeMemberFromProjectCheck" +
                "|$appCode|$userId|$projectId|$targetMemberId"
        )
        checkProjectManager(projectId, userId)
        return client.get(ServiceResourceMemberResource::class)
            .removeMemberFromProjectCheck(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = RemoveMemberFromProjectReq(
                    targetMember = ResourceMemberInfo(
                        id = targetMemberId,
                        type = USER_TYPE
                    ),
                    handoverTo = null
                )
            )
    }

    private fun checkProjectManager(
        projectId: String,
        userId: String
    ) {
        client.get(ServiceProjectAuthResource::class)
            .checkProjectManagerAndMessage(
                projectId = projectId,
                userId = userId
            )
    }
}
