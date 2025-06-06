package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.response.MemberGroupDetailsResponse
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_HANDOVER_APPROVAL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_HANDOVER_FINISH
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_HANDOVER_HANDLE
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_HANDOVER_REVOKE
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_SINGLE_GROUP_REMOVE
import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.HandoverDetailDTO
import com.tencent.devops.auth.pojo.dto.HandoverOverviewCreateDTO
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.InvalidAuthorizationsDTO
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.dto.ProjectMembersQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.HandoverAction
import com.tencent.devops.auth.pojo.enum.HandoverQueryChannel
import com.tencent.devops.auth.pojo.enum.HandoverStatus
import com.tencent.devops.auth.pojo.enum.HandoverType
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.enum.RemoveMemberButtonControl
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewBatchUpdateReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ResourceType2CountOfHandoverQuery
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.iam.PermissionHandoverApplicationService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.lock.HandleHandoverApplicationLock
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.enums.HandoverChannelCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("ComplexCondition")
class RbacPermissionManageFacadeServiceImpl(
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val groupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService,
    private val iamV2ManagerService: V2ManagerService,
    private val authAuthorizationDao: AuthAuthorizationDao,
    private val syncIamGroupMemberService: PermissionResourceGroupSyncService,
    private val permissionAuthorizationService: PermissionAuthorizationService,
    private val permissionHandoverApplicationService: PermissionHandoverApplicationService,
    private val rbacCommonService: RbacCommonService,
    private val redisOperation: RedisOperation,
    private val authorizationDao: AuthAuthorizationDao,
    private val authResourceService: AuthResourceService,
    private val client: Client,
    private val config: CommonConfig
) : PermissionManageFacadeService {
    override fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?,
        start: Int?,
        limit: Int?
    ): SQLPage<GroupDetailsInfoVo> {
        // 根据查询条件查询得到iam组id
        val iamGroupIdsByConditions = listIamGroupIdsByConditions(
            condition = IamGroupIdsQueryConditionDTO(
                projectCode = projectId,
                groupName = groupName,
                iamGroupIds = iamGroupIds,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action,
                uniqueManagerGroupsQueryFlag = uniqueManagerGroupsQueryFlag
            )
        )
        // 查询成员所在资源用户组列表
        val (count, resourceGroupMembers) = listResourceGroupMembers(
            projectCode = projectId,
            memberId = memberId,
            resourceType = resourceType,
            iamGroupIds = iamGroupIdsByConditions,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            operateChannel = operateChannel,
            start = start,
            limit = limit
        )
        // 用户组对应的资源信息
        val resourceGroupMap = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = resourceGroupMembers.map { it.iamGroupId.toString() }
        ).associateBy { it.relationId }
        // 只有一个成员的管理员组
        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = resourceGroupMembers.map { it.iamGroupId }
        )
        // 用户组成员详情
        val groupMemberDetailMap = getGroupMemberDetailMap(
            memberId = memberId,
            resourceGroupMembers = resourceGroupMembers,
            operateChannel = operateChannel
        )
        // 获取用户正在交接的用户组，仅用于个人视角
        val groupsBeingHandover = if (operateChannel == OperateChannel.PERSONAL) {
            permissionHandoverApplicationService.listMemberHandoverDetails(
                projectCode = projectId,
                memberId = memberId,
                handoverType = HandoverType.GROUP
            )
        } else {
            emptyList()
        }
        val records = mutableListOf<GroupDetailsInfoVo>()
        resourceGroupMembers.forEach {
            val resourceGroup = resourceGroupMap[it.iamGroupId.toString()]!!
            val groupMemberDetail = groupMemberDetailMap["${it.iamGroupId}_${it.memberId}"]
            records.add(
                convertGroupDetailsInfoVo(
                    resourceGroup = resourceGroup,
                    groupMemberDetail = groupMemberDetail,
                    uniqueManagerGroups = uniqueManagerGroups,
                    authResourceGroupMember = it,
                    operateChannel = operateChannel,
                    groupsBeingHandover = groupsBeingHandover
                )
            )
        }
        return SQLPage(count = count, records = records)
    }

    private fun getGroupMemberDetailMap(
        memberId: String,
        resourceGroupMembers: List<AuthResourceGroupMember>,
        operateChannel: OperateChannel?
    ): Map<String, MemberGroupDetailsResponse> {
        // 如果用户离职，查询权限中心接口会报错
        if (deptService.isUserDeparted(memberId)) {
            return emptyMap()
        }
        // 用户组成员详情
        val groupMemberDetailMap = mutableMapOf<String, MemberGroupDetailsResponse>()
        // 直接加入的用户
        val userGroupIds = resourceGroupMembers
            .filter { it.memberType == MemberType.USER.type }
            .map { it.iamGroupId }
        if (userGroupIds.isNotEmpty()) {
            iamV2ManagerService.listMemberGroupsDetails(
                MemberType.USER.type,
                memberId,
                userGroupIds.joinToString(",")
            ).forEach {
                groupMemberDetailMap["${it.id}_$memberId"] = it
            }
        }
        val deptGroups = resourceGroupMembers
            .filter { it.memberType == MemberType.DEPARTMENT.type }
        when {
            deptGroups.isEmpty() -> {}
            operateChannel == OperateChannel.PERSONAL -> {
                // 个人视角，会获取用户通过组织间接加入的组
                deptGroups.groupBy({ it.memberId }, { it.iamGroupId.toString() })
                    .forEach { (deptId, iamGroupIds) ->
                        if (iamGroupIds.isEmpty()) return@forEach
                        iamV2ManagerService.listMemberGroupsDetails(
                            MemberType.DEPARTMENT.type,
                            deptId,
                            iamGroupIds.joinToString(",")
                        ).forEach {
                            groupMemberDetailMap["${it.id}_$deptId"] = it
                        }
                    }
            }

            else -> {
                // 管理员视角，获取组织直接加入的用户组
                val deptGroupIds = deptGroups.map { it.iamGroupId }
                iamV2ManagerService.listMemberGroupsDetails(
                    MemberType.DEPARTMENT.type,
                    memberId,
                    deptGroupIds.joinToString(",")
                ).forEach {
                    groupMemberDetailMap["${it.id}_$memberId"] = it
                }
            }
        }
        // 人员模板加入的组
        resourceGroupMembers.filter { it.memberType == MemberType.TEMPLATE.type }
            .groupBy({ it.memberId }, { it.iamGroupId.toString() })
            .forEach { (iamTemplateId, iamGroupIds) ->
                if (iamGroupIds.isEmpty()) return@forEach
                iamV2ManagerService.listMemberGroupsDetails(
                    MemberType.TEMPLATE.type,
                    iamTemplateId,
                    iamGroupIds.joinToString(",")
                ).forEach {
                    groupMemberDetailMap["${it.id}_$iamTemplateId"] = it
                }
            }
        return groupMemberDetailMap
    }

    private fun convertGroupDetailsInfoVo(
        resourceGroup: TAuthResourceGroupRecord,
        groupMemberDetail: MemberGroupDetailsResponse?,
        uniqueManagerGroups: List<Int>,
        authResourceGroupMember: AuthResourceGroupMember,
        operateChannel: OperateChannel?,
        groupsBeingHandover: List<HandoverDetailDTO>
    ): GroupDetailsInfoVo {
        // 如果用户离职，查询权限中心接口会报错，因此从数据库直接取数据，而不去调用权限中心接口。
        val (expiredAt, joinedTime) = if (groupMemberDetail != null) {
            Pair(
                TimeUnit.SECONDS.toMillis(groupMemberDetail.expiredAt),
                TimeUnit.SECONDS.toMillis(groupMemberDetail.createdAt)
            )
        } else {
            Pair(
                authResourceGroupMember.expiredTime.timestampmilli(),
                0L
            )
        }
        val between = expiredAt - System.currentTimeMillis()
        val groupId = resourceGroup.relationId.toInt()
        return GroupDetailsInfoVo(
            resourceCode = resourceGroup.resourceCode,
            resourceName = resourceGroup.resourceName,
            resourceType = resourceGroup.resourceType,
            groupId = groupId,
            groupName = resourceGroup.groupName,
            groupDesc = resourceGroup.description,
            expiredAtDisplay = when {
                expiredAt == PERMANENT_EXPIRED_TIME ->
                    I18nUtil.getCodeLanMessage(messageCode = AuthI18nConstants.BK_MEMBER_EXPIRED_AT_DISPLAY_PERMANENT)

                between >= 0 -> I18nUtil.getCodeLanMessage(
                    messageCode = AuthI18nConstants.BK_MEMBER_EXPIRED_AT_DISPLAY_NORMAL,
                    params = arrayOf(DateTimeUtil.formatDay(between))
                )

                else -> I18nUtil.getCodeLanMessage(
                    messageCode = AuthI18nConstants.BK_MEMBER_EXPIRED_AT_DISPLAY_EXPIRED
                )
            },
            expiredAt = expiredAt,
            joinedTime = joinedTime,
            removeMemberButtonControl = when {
                authResourceGroupMember.memberType == MemberType.TEMPLATE.type ->
                    RemoveMemberButtonControl.TEMPLATE

                operateChannel == OperateChannel.PERSONAL &&
                    authResourceGroupMember.memberType == MemberType.DEPARTMENT.type ->
                    RemoveMemberButtonControl.DEPARTMENT

                resourceGroup.resourceType == AuthResourceType.PROJECT.value &&
                    uniqueManagerGroups.contains(authResourceGroupMember.iamGroupId) ->
                    RemoveMemberButtonControl.UNIQUE_MANAGER

                uniqueManagerGroups.contains(authResourceGroupMember.iamGroupId) ->
                    RemoveMemberButtonControl.UNIQUE_OWNER

                else ->
                    RemoveMemberButtonControl.OTHER
            },
            joinedType = when {
                authResourceGroupMember.memberType == MemberType.TEMPLATE.type -> JoinedType.TEMPLATE
                authResourceGroupMember.memberType == MemberType.DEPARTMENT.type &&
                    operateChannel == OperateChannel.PERSONAL -> JoinedType.DEPARTMENT

                else -> JoinedType.DIRECT
            },
            operator = "",
            beingHandedOver = groupsBeingHandover.map { it.itemId.toInt() }.contains(groupId),
            flowNo = groupsBeingHandover.firstOrNull { it.itemId.toInt() == groupId }?.flowNo,
            memberType = MemberType.get(authResourceGroupMember.memberType)
        )
    }

    override fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?
    ): List<ResourceType2CountVo> {
        val iamGroupIdsByConditions = listIamGroupIdsByConditions(
            condition = IamGroupIdsQueryConditionDTO(
                projectCode = projectCode,
                groupName = groupName,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action,
                uniqueManagerGroupsQueryFlag = uniqueManagerGroupsQueryFlag
            )
        )

        val (iamTemplateIds, memberDeptInfos) = getMemberTemplateIdsAndDeptInfos(
            projectCode = projectCode,
            memberId = memberId,
            operateChannel = operateChannel
        )
        // 获取成员加入的用户组
        val memberGroupCountMap = authResourceGroupMemberDao.countMemberGroupOfResourceType(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            iamGroupIds = iamGroupIdsByConditions,
            minExpiredAt = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) },
            maxExpiredAt = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) },
            memberDeptInfos = memberDeptInfos
        )
        return rbacCommonService.convertResourceType2Count(memberGroupCountMap)
    }

    private fun getMemberTemplateIdsAndDeptInfos(
        projectCode: String,
        memberId: String,
        operateChannel: OperateChannel?
    ): Pair<List<String>, List<String>> {
        // 获取用户加入的项目级用户组模板ID
        val iamTemplateIds = listProjectMemberGroupTemplateIds(
            projectCode = projectCode,
            memberId = memberId
        )
        // 获取用户部门信息
        val memberDeptInfos = if (operateChannel == OperateChannel.PERSONAL) {
            getMemberDeptInfos(memberId)
        } else {
            emptyList()
        }
        return Pair(iamTemplateIds, memberDeptInfos)
    }

    override fun listIamGroupIdsByConditions(condition: IamGroupIdsQueryConditionDTO): List<Int> {
        val finalGroupIds = mutableListOf<Int>()

        // 处理按组名查询的情况
        if (condition.isQueryByGroupName()) {
            val groupIdsByGroupName = permissionResourceGroupService.listIamGroupIdsByGroupName(
                projectId = condition.projectCode,
                groupName = condition.groupName!!
            )
            finalGroupIds.addAll(groupIdsByGroupName)
        }

        // 处理按权限条件查询的情况
        if (condition.isQueryByGroupPermissions()) {
            val groupsByPermissions = groupPermissionService.listGroupsByPermissionConditions(
                projectCode = condition.projectCode,
                filterIamGroupIds = finalGroupIds,
                relatedResourceType = condition.relatedResourceType!!,
                relatedResourceCode = condition.relatedResourceCode,
                action = condition.action
            )
            finalGroupIds.clear()
            finalGroupIds.addAll(groupsByPermissions)
        }

        // 添加额外的 IAM 组 ID（如果有）
        condition.iamGroupIds?.let { finalGroupIds.addAll(it) }

        // 如果需要唯一管理组查询，则过滤出唯一的组
        if (condition.uniqueManagerGroupsQueryFlag == true) {
            val groupsByUniqueManager = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                dslContext = dslContext,
                projectCode = condition.projectCode,
                iamGroupIds = finalGroupIds
            )

            finalGroupIds.clear()
            finalGroupIds.addAll(groupsByUniqueManager)
        }

        return finalGroupIds
    }

    override fun listMemberGroupIdsInProject(
        projectCode: String,
        memberId: String
    ): List<Int> {
        // 获取用户加入的项目级用户组模板ID
        val iamTemplateIds = listProjectMemberGroupTemplateIds(
            projectCode = projectCode,
            memberId = memberId
        )
        return authResourceGroupMemberDao.listMemberGroupIdsInProject(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds
        )
    }

    @Suppress("LongParameterList")
    override fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        operateChannel: OperateChannel?,
        filterMemberType: MemberType?,
        excludeIamGroupIds: List<Int>?,
        onlyExcludeUserDirectlyJoined: Boolean?,
        start: Int?,
        limit: Int?
    ): Pair<Long, List<AuthResourceGroupMember>> {
        // 获取用户的部门信息以及加入的项目级别用户组模板ID
        val (iamTemplateIds, memberDeptInfos) = getMemberTemplateIdsAndDeptInfos(
            projectCode = projectCode,
            memberId = memberId,
            operateChannel = operateChannel
        )

        val minExpiredTime = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val maxExpiredTime = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val count = authResourceGroupMemberDao.countMemberGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredTime,
            maxExpiredAt = maxExpiredTime,
            memberDeptInfos = memberDeptInfos,
            filterMemberType = filterMemberType,
            excludeIamGroupIds = excludeIamGroupIds,
            onlyExcludeUserDirectlyJoined = onlyExcludeUserDirectlyJoined
        )
        val resourceGroupMembers = authResourceGroupMemberDao.listMemberGroupDetail(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredTime,
            maxExpiredAt = maxExpiredTime,
            memberDeptInfos = memberDeptInfos,
            filterMemberType = filterMemberType,
            excludeIamGroupIds = excludeIamGroupIds,
            onlyExcludeUserDirectlyJoined = onlyExcludeUserDirectlyJoined,
            offset = start,
            limit = limit
        )
        return Pair(count, resourceGroupMembers)
    }

    // 获取用户加入的项目级用户组模板ID
    private fun listProjectMemberGroupTemplateIds(
        projectCode: String,
        memberId: String
    ): List<String> {
        // 查询项目下包含该成员的组列表
        val projectGroupIds = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            memberId = memberId
        ).map { it.iamGroupId.toString() }
        // 通过项目组ID获取人员模板ID
        return authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = projectGroupIds
        ).filter { it.iamTemplateId != null }
            .map { it.iamTemplateId.toString() }
    }

    private fun getMemberDeptInfos(
        memberId: String
    ): List<String> {
        deptService.getUserInfo(
            userId = "admin",
            name = memberId
        )?.deptInfo ?: return emptyList()
        return deptService.getUserDeptInfo(memberId).toList()
    }

    private fun getGroupIdsByGroupMemberCondition(
        projectCode: String,
        commonCondition: GroupMemberCommonConditionReq,
        minExpiredAt: Long? = null
    ): Map<MemberType, List<Int>> {
        val finalMemberGroups = mutableListOf<AuthResourceGroupMember>()

        val resourceGroupMembersByCondition = when {
            commonCondition.allSelection -> {
                listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = commonCondition.targetMember.id,
                    operateChannel = commonCondition.operateChannel,
                    minExpiredAt = minExpiredAt
                ).second
            }

            commonCondition.resourceTypes.isNotEmpty() -> {
                commonCondition.resourceTypes.flatMap { resourceType ->
                    listResourceGroupMembers(
                        projectCode = projectCode,
                        memberId = commonCondition.targetMember.id,
                        resourceType = resourceType,
                        operateChannel = commonCondition.operateChannel,
                        minExpiredAt = minExpiredAt
                    ).second
                }
            }

            else -> emptyList()
        }

        finalMemberGroups.addAll(resourceGroupMembersByCondition)
        if (commonCondition.groupIds.isNotEmpty()) {
            val memberType2groupIds = commonCondition.groupIds.groupBy { it.memberType }
            memberType2groupIds.forEach { (memberType, groupIds) ->
                val groupsOfSelect = listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = commonCondition.targetMember.id,
                    iamGroupIds = groupIds.map { it.id },
                    operateChannel = commonCondition.operateChannel,
                    filterMemberType = memberType,
                    minExpiredAt = minExpiredAt
                ).second
                finalMemberGroups.addAll(groupsOfSelect)
            }
        }
        // 分类
        val result = mutableMapOf<MemberType, List<Int>>()
        finalMemberGroups.groupBy { it.memberType }.forEach { (memberType, groups) ->
            result[MemberType.get(memberType)] = groups.map { it.iamGroupId }
        }
        return result
    }

    override fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo> {
        logger.info("list project members by complex conditions: $conditionReq")
        // 不允许同时查询部门名称和用户名称
        if (conditionReq.userName != null && conditionReq.deptName != null) {
            return SQLPage(count = 0, records = emptyList())
        }

        // 简单查询直接返回结果
        if (!conditionReq.isComplexQuery()) {
            return permissionResourceMemberService.listProjectMembers(
                projectCode = conditionReq.projectCode,
                memberType = conditionReq.memberType,
                userName = conditionReq.userName,
                deptName = conditionReq.deptName,
                departedFlag = conditionReq.departedFlag,
                page = conditionReq.page,
                pageSize = conditionReq.pageSize
            )
        }

        // 处理复杂查询条件
        val iamGroupIdsByCondition = if (conditionReq.isNeedToQueryIamGroups()) {
            listIamGroupIdsByConditions(
                condition = IamGroupIdsQueryConditionDTO(
                    projectCode = conditionReq.projectCode,
                    groupName = conditionReq.groupName,
                    relatedResourceType = conditionReq.relatedResourceType,
                    relatedResourceCode = conditionReq.relatedResourceCode,
                    action = conditionReq.action
                )
            )
        } else {
            emptyList()
        }.toMutableList()

        // 查询不到用户组，直接返回空
        if (conditionReq.isNeedToQueryIamGroups() && iamGroupIdsByCondition.isEmpty()) {
            return SQLPage(0, emptyList())
        }

        val conditionDTO = ProjectMembersQueryConditionDTO.build(conditionReq, iamGroupIdsByCondition)

        if (iamGroupIdsByCondition.isNotEmpty()) {
            logger.debug("iamGroupIdsByCondition :{}", iamGroupIdsByCondition)
            // 根据用户组Id查询出对应用户组中的人员模板成员
            val iamTemplateIds = authResourceGroupMemberDao.listProjectMembersByComplexConditions(
                dslContext = dslContext,
                conditionDTO = ProjectMembersQueryConditionDTO(
                    projectCode = conditionDTO.projectCode,
                    queryTemplate = true,
                    iamGroupIds = conditionDTO.iamGroupIds
                )
            )
            if (iamTemplateIds.isNotEmpty()) {
                // 根据查询出的人员模板ID，查询出对应的组ID
                val iamGroupIdsFromTemplate = authResourceGroupDao.listIamGroupIdsByConditions(
                    dslContext = dslContext,
                    projectCode = conditionDTO.projectCode,
                    iamTemplateIds = iamTemplateIds.map { it.id.toInt() }
                )
                iamGroupIdsByCondition.addAll(iamGroupIdsFromTemplate)
                logger.debug("iamGroupIdsByCondition and template :{}", iamGroupIdsByCondition)
            }
        }

        val records = authResourceGroupMemberDao.listProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )
        logger.debug("listProjectMembersByComplexConditions :{}", records)

        val count = authResourceGroupMemberDao.countProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )
        logger.debug("listProjectMembersByComplexConditions :$count")
        // 添加离职标志
        return if (conditionDTO.departedFlag == false) {
            SQLPage(count, records)
        } else {
            SQLPage(count, permissionResourceMemberService.addDepartedFlagToMembers(records))
        }
    }

    override fun listInvalidAuthorizationsAfterOperatedGroups(
        projectCode: String,
        iamGroupIdsOfDirectlyJoined: List<Int>,
        memberId: String
    ): InvalidAuthorizationsDTO {
        val startEpoch = System.currentTimeMillis()
        try {
            if (iamGroupIdsOfDirectlyJoined.isEmpty()) {
                return InvalidAuthorizationsDTO(
                    invalidGroupIds = emptyList(),
                    invalidPipelineIds = emptyList(),
                    invalidRepertoryIds = emptyList(),
                    invalidEnvNodeIds = emptyList()
                )
            }

            // 筛选出本次操作中未过期的用户组
            val iamGroupIdsOfNotExpired = getNotExpiredIamGroupIds(
                projectCode = projectCode,
                memberId = memberId,
                iamGroupIds = iamGroupIdsOfDirectlyJoined
            )
            // 获取用户退出/交接以上用户组后，还未退出的用户组（包含组织/直接/模板加入的组）
            val (count, userGroupsJoinedAfterOperatedGroups) = listResourceGroupMembers(
                projectCode = projectCode,
                memberId = memberId,
                excludeIamGroupIds = iamGroupIdsOfDirectlyJoined,
                onlyExcludeUserDirectlyJoined = true,
                operateChannel = OperateChannel.PERSONAL,
                minExpiredAt = LocalDateTime.now().timestampmilli()
            )
            logger.debug(
                "list all user groups joined after operated groups: {}, {}",
                count, userGroupsJoinedAfterOperatedGroups
            )

            val isHasProjectVisitPermAfterOperatedGroups = checkProjectVisitPermission(
                projectCode = projectCode,
                iamGroupIds = userGroupsJoinedAfterOperatedGroups.map { it.iamGroupId }
            )
            logger.debug(
                "whether the user has project visit perm after operated groups: {}",
                isHasProjectVisitPermAfterOperatedGroups
            )

            val invalidAuthorizationsDTO = if (count == 0L || !isHasProjectVisitPermAfterOperatedGroups) {
                // 若用户已退出了所有的用户组或失去了项目访问权限，则直接返回项目下所有的授权
                getInvalidAuthorizationsAfterAllGroupsRemoved(
                    projectCode = projectCode,
                    memberId = memberId,
                    iamGroupIdsOfNotExpired = iamGroupIdsOfNotExpired
                )
            } else {
                val (invalidGroups, invalidPipelines) = getInvalidPipelinesAfterOperatedGroups(
                    projectCode = projectCode,
                    iamGroupIds = iamGroupIdsOfDirectlyJoined,
                    memberId = memberId,
                    iamGroupIdsOfNotExpired = iamGroupIdsOfNotExpired
                )
                InvalidAuthorizationsDTO(
                    invalidGroupIds = invalidGroups,
                    invalidPipelineIds = invalidPipelines
                )
            }
            logger.info(
                "invalid authorizations after operated groups|$projectCode|$iamGroupIdsOfDirectlyJoined|$memberId|" +
                    "$invalidAuthorizationsDTO"
            )
            return invalidAuthorizationsDTO
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to check invalid authorizations " +
                    "after operated groups |$projectCode|$iamGroupIdsOfDirectlyJoined|$memberId"
            )
        }
    }

    private fun getNotExpiredIamGroupIds(
        projectCode: String,
        memberId: String,
        iamGroupIds: List<Int>
    ): List<Int> {
        return authResourceGroupMemberDao.listMemberGroupDetail(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamGroupIds = iamGroupIds,
            minExpiredAt = LocalDateTime.now()
        ).map { it.iamGroupId }
    }

    private fun checkProjectVisitPermission(
        projectCode: String,
        iamGroupIds: List<Int>
    ): Boolean {
        return groupPermissionService.isGroupsHasPermission(
            projectCode = projectCode,
            filterIamGroupIds = iamGroupIds,
            relatedResourceType = ResourceTypeId.PROJECT,
            relatedResourceCode = projectCode,
            action = ActionId.PROJECT_VISIT
        )
    }

    private fun getInvalidAuthorizationsAfterAllGroupsRemoved(
        projectCode: String,
        memberId: String,
        iamGroupIdsOfNotExpired: List<Int>
    ): InvalidAuthorizationsDTO {
        val invalidAuthorizations = authAuthorizationDao.list(
            dslContext = dslContext,
            condition = ResourceAuthorizationConditionRequest(
                projectCode = projectCode,
                handoverFrom = memberId
            )
        ).groupBy({ it.resourceType }, { it.resourceCode })

        val operatedGroupsWithExecutePerm = groupPermissionService.listGroupsByPermissionConditions(
            projectCode = projectCode,
            relatedResourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            action = ActionId.PIPELINE_EXECUTE,
            filterIamGroupIds = iamGroupIdsOfNotExpired
        )
        val invalidGroupIds = if (invalidAuthorizations.isNotEmpty()) {
            operatedGroupsWithExecutePerm
        } else {
            emptyList()
        }
        return InvalidAuthorizationsDTO(
            invalidGroupIds = invalidGroupIds,
            invalidPipelineIds = invalidAuthorizations[ResourceTypeId.PIPELINE] ?: emptyList(),
            invalidRepertoryIds = invalidAuthorizations[ResourceTypeId.REPERTORY] ?: emptyList(),
            invalidEnvNodeIds = invalidAuthorizations[ResourceTypeId.ENV_NODE] ?: emptyList()
        )
    }

    private fun getInvalidPipelinesAfterOperatedGroups(
        projectCode: String,
        iamGroupIds: List<Int>,
        memberId: String,
        iamGroupIdsOfNotExpired: List<Int>
    ): InvalidAuthorizationsDTO {
        logger.info("list invalid authorizations after operated groups:$projectCode|$iamGroupIds|$memberId")
        val now = LocalDateTime.now()
        logger.debug("list iam group ids of not expired:{}", iamGroupIdsOfNotExpired)
        // 1.筛选出本次退出/交接中包含流水线执行权限的用户组
        val operatedGroupsWithExecutePerm = groupPermissionService.listGroupsByPermissionConditions(
            projectCode = projectCode,
            relatedResourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            action = ActionId.PIPELINE_EXECUTE,
            filterIamGroupIds = iamGroupIdsOfNotExpired
        )
        logger.debug("list operated groups with execute perm:{}", operatedGroupsWithExecutePerm)
        if (operatedGroupsWithExecutePerm.isEmpty()) {
            return InvalidAuthorizationsDTO(emptyList(), emptyList())
        }

        // 2.获取用户退出/交接以上操作的用户组后，还未退出并且未过期的流水线/项目级别（仅这些类型会包含流水线执行权限）的用户组。
        val userGroupsJoinedAfterOperatedGroups = listResourceGroupMembers(
            projectCode = projectCode,
            memberId = memberId,
            resourceType = ResourceTypeId.PIPELINE,
            excludeIamGroupIds = iamGroupIdsOfNotExpired,
            operateChannel = OperateChannel.PERSONAL,
            onlyExcludeUserDirectlyJoined = true,
            minExpiredAt = now.timestampmilli()
        ).second.toMutableList().apply {
            addAll(
                listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = memberId,
                    resourceType = ResourceTypeId.PROJECT,
                    excludeIamGroupIds = iamGroupIdsOfNotExpired,
                    operateChannel = OperateChannel.PERSONAL,
                    onlyExcludeUserDirectlyJoined = true,
                    minExpiredAt = now.timestampmilli()
                ).second
            )
        }.map { it.iamGroupId }
        logger.debug(
            "list pipeline and project groups joined after operated groups:{}",
            userGroupsJoinedAfterOperatedGroups
        )

        // 3.查询未退出的流水线/项目级别的用户组中是否包含项目级别的流水线执行权限。
        val hasAllPipelineExecutePermAfterOperateGroups = groupPermissionService.isGroupsHasProjectLevelPermission(
            projectCode = projectCode,
            filterIamGroupIds = userGroupsJoinedAfterOperatedGroups,
            action = ActionId.PIPELINE_EXECUTE
        )
        logger.debug(
            "has all pipeline execute perm after operate groups:{}",
            hasAllPipelineExecutePermAfterOperateGroups
        )

        // 3.1.若用户在未退出的组中拥有整个项目的流水线执行权限，则本次不会对任何的流水线代持人权限造成影响。
        if (hasAllPipelineExecutePermAfterOperateGroups)
            return InvalidAuthorizationsDTO(emptyList(), emptyList())

        // 3.2.若不包含整个项目的流水线执行权限，需查询本次退出/交接的用户组中是否包含项目级别的流水线执行权限。
        val hasAllPipelineExecutePermInOperateGroups = groupPermissionService.isGroupsHasProjectLevelPermission(
            projectCode = projectCode,
            filterIamGroupIds = operatedGroupsWithExecutePerm,
            action = ActionId.PIPELINE_EXECUTE
        )
        logger.debug("has all pipeline execute perm in operate groups:{}", hasAllPipelineExecutePermInOperateGroups)

        val pipelinesWithoutAuthorization = if (hasAllPipelineExecutePermInOperateGroups) {
            // 3.2.1 如果本次退出/交接的用户组中包含项目级别的流水线执行权限，
            // 那么查询出用户还有执行流水线权限的流水线，该项目下除了这些流水线，其他的流水线代持人权限都会失效。
            val userHasExecutePermAfterOperatedGroups = groupPermissionService.listGroupResourcesWithPermission(
                projectCode = projectCode,
                filterIamGroupIds = userGroupsJoinedAfterOperatedGroups,
                relatedResourceType = ResourceTypeId.PIPELINE,
                action = ActionId.PIPELINE_EXECUTE
            )[ResourceTypeId.PIPELINE] ?: emptyList()
            logger.debug("user has execute perm after operated groups:{}", userHasExecutePermAfterOperatedGroups)
            // 失去代持人权限的流水线
            authAuthorizationDao.list(
                dslContext = dslContext,
                condition = ResourceAuthorizationConditionRequest(
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.PIPELINE,
                    handoverFrom = memberId,
                    excludeResourceCodes = userHasExecutePermAfterOperatedGroups
                )
            ).map { it.resourceCode }
        } else {
            // 3.2.2 如果本次退出/交接的用户组中不包含整个项目的流水线执行权限。
            // 通过计算得出，用户本次操作用户组，导致失去流水线执行权限的流水线。
            // 然后再计算失去这些流水线执行权限后，会导致哪些流水线的代持人权限失效。
            val pipelinesWithExecutePermAfterOperatedGroups = groupPermissionService.listGroupResourcesWithPermission(
                projectCode = projectCode,
                filterIamGroupIds = userGroupsJoinedAfterOperatedGroups,
                relatedResourceType = ResourceTypeId.PIPELINE,
                action = ActionId.PIPELINE_EXECUTE
            )[ResourceTypeId.PIPELINE] ?: emptyList()
            logger.debug(
                "pipelines with execute perm after operate groups:{}",
                pipelinesWithExecutePermAfterOperatedGroups
            )

            val pipelinesWithExecutePermInOperateGroups = groupPermissionService.listGroupResourcesWithPermission(
                projectCode = projectCode,
                filterIamGroupIds = operatedGroupsWithExecutePerm,
                relatedResourceType = ResourceTypeId.PIPELINE,
                action = ActionId.PIPELINE_EXECUTE
            )[ResourceTypeId.PIPELINE] ?: emptyList()
            logger.debug("pipelines with execute perm in operate groups:{}", pipelinesWithExecutePermInOperateGroups)

            val pipelineExecutePermLostFromUser = pipelinesWithExecutePermInOperateGroups.filterNot {
                pipelinesWithExecutePermAfterOperatedGroups.contains(it)
            }
            // 失去代持人权限的流水线
            authAuthorizationDao.list(
                dslContext = dslContext,
                condition = ResourceAuthorizationConditionRequest(
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.PIPELINE,
                    handoverFrom = memberId,
                    filterResourceCodes = pipelineExecutePermLostFromUser
                )
            ).map { it.resourceCode }
        }
        logger.debug("pipelines without authorization:{}", pipelinesWithoutAuthorization)
        if (pipelinesWithoutAuthorization.isNotEmpty()) {
            return InvalidAuthorizationsDTO(
                invalidGroupIds = operatedGroupsWithExecutePerm,
                invalidPipelineIds = pipelinesWithoutAuthorization
            )
        }

        return InvalidAuthorizationsDTO(emptyList(), emptyList())
    }

    override fun renewalGroupMember(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Boolean {
        logger.info("renewal group member $userId|$projectCode|$renewalConditionReq")
        val groupId = renewalConditionReq.groupId
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.RENEWAL,
            conditionReq = GroupMemberRenewalConditionReq(
                groupIds = listOf(
                    MemberGroupJoinedDTO(
                        id = groupId,
                        memberType = MemberType.get(renewalConditionReq.targetMember.type)
                    )
                ),
                targetMember = renewalConditionReq.targetMember,
                renewalDuration = renewalConditionReq.renewalDuration
            ),
            operateGroupMemberTask = ::renewalTask
        )
        return true
    }

    private fun renewalTask(
        projectCode: String,
        groupId: Int,
        renewalConditionReq: GroupMemberRenewalConditionReq,
        expiredAt: Long
    ) {
        logger.info("renewal group member ${renewalConditionReq.targetMember}|$projectCode|$groupId|$expiredAt")
        val targetMember = renewalConditionReq.targetMember
        if (targetMember.type == MemberType.USER.type && deptService.isUserDeparted(targetMember.id)) {
            return
        }
        val secondsOfRenewalDuration = TimeUnit.DAYS.toSeconds(renewalConditionReq.renewalDuration.toLong())
        val secondsOfCurrentTime = System.currentTimeMillis() / 1000
        // 若权限已过期，则为当前时间+续期天数，若未过期，则为有效期+续期天数
        val finalExpiredAt = if (expiredAt < secondsOfCurrentTime) {
            secondsOfCurrentTime
        } else {
            expiredAt
        } + secondsOfRenewalDuration
        if (!isNeedToRenewal(finalExpiredAt)) {
            return
        }
        permissionResourceMemberService.renewalIamGroupMembers(
            groupId = groupId,
            members = listOf(ManagerMember(targetMember.type, targetMember.id)),
            expiredAt = finalExpiredAt
        )
        authResourceGroupMemberDao.update(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = groupId,
            expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(finalExpiredAt),
            memberId = targetMember.id
        )
    }

    private fun isNeedToRenewal(expiredAt: Long): Boolean {
        return expiredAt < PERMANENT_EXPIRED_TIME / 1000
    }

    override fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean {
        logger.info("batch renewal group member $userId|$projectCode|$renewalConditionReq")
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.RENEWAL,
            conditionReq = renewalConditionReq,
            operateGroupMemberTask = ::renewalTask
        )
        return true
    }

    override fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Boolean {
        logger.info("batch handover group members from manager $userId|$projectCode|$handoverMemberDTO")
        handoverMemberDTO.checkHandoverTo()
        // 若交接对象是部门，直接进行交接
        if (handoverMemberDTO.targetMember.type == MemberType.DEPARTMENT.type) {
            batchOperateGroupMembers(
                projectCode = projectCode,
                type = BatchOperateType.HANDOVER,
                conditionReq = handoverMemberDTO,
                operateGroupMemberTask = ::handoverTask
            )
        }
        // 若操作对象是用户，需要将被影响流水线授权一并交接给授权人
        val groupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = handoverMemberDTO
        )[MemberType.USER] ?: return true
        // 获取导致失效的流水线/代码库授权/环境节点授权，并进行交接
        val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) =
            listInvalidAuthorizationsAfterOperatedGroups(
                projectCode = projectCode,
                iamGroupIdsOfDirectlyJoined = groupIds,
                memberId = handoverMemberDTO.targetMember.id
            )
        // 检查授予人是否有代码库oauth权限
        if (handoverMemberDTO.checkRepertoryAuthorization && invalidRepertoryIds.isNotEmpty()) {
            permissionAuthorizationService.checkRepertoryAuthorizationsHanover(
                operator = userId,
                projectCode = projectCode,
                repertoryIds = invalidRepertoryIds,
                handoverFrom = handoverMemberDTO.targetMember.id,
                handoverTo = handoverMemberDTO.handoverTo.id
            )
        }
        // 交接用户组
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.HANDOVER,
            conditionReq = handoverMemberDTO,
            operateGroupMemberTask = ::handoverTask
        )
        handoverAuthorizationsWhenOperatedGroups(
            userId = userId,
            projectCode = projectCode,
            invalidPipelines = invalidPipelines,
            invalidRepertoryIds = invalidRepertoryIds,
            invalidEnvNodeIds = invalidEnvNodeIds,
            handoverFrom = handoverMemberDTO.targetMember.id,
            handoverTo = handoverMemberDTO.handoverTo.id
        )
        return true
    }

    private fun handoverAuthorizationsWhenOperatedGroups(
        userId: String,
        projectCode: String,
        invalidRepertoryIds: List<String>,
        invalidPipelines: List<String>,
        invalidEnvNodeIds: List<String>,
        handoverFrom: String,
        handoverTo: String
    ) {
        if (invalidRepertoryIds.isNotEmpty()) {
            permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                operator = userId,
                projectCode = projectCode,
                condition = ResourceAuthorizationHandoverConditionRequest(
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.REPERTORY,
                    fullSelection = true,
                    filterResourceCodes = invalidRepertoryIds,
                    handoverChannel = HandoverChannelCode.MANAGER,
                    handoverFrom = handoverFrom,
                    handoverTo = handoverTo,
                    checkPermission = false
                )
            )
        }
        if (invalidPipelines.isNotEmpty()) {
            permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                operator = userId,
                projectCode = projectCode,
                condition = ResourceAuthorizationHandoverConditionRequest(
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.PIPELINE,
                    fullSelection = true,
                    filterResourceCodes = invalidPipelines,
                    handoverChannel = HandoverChannelCode.MANAGER,
                    handoverFrom = handoverFrom,
                    handoverTo = handoverTo,
                    checkPermission = false
                )
            )
        }
        if (invalidEnvNodeIds.isNotEmpty()) {
            permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                operator = userId,
                projectCode = projectCode,
                condition = ResourceAuthorizationHandoverConditionRequest(
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.ENV_NODE,
                    fullSelection = true,
                    filterResourceCodes = invalidEnvNodeIds,
                    handoverChannel = HandoverChannelCode.MANAGER,
                    handoverFrom = handoverFrom,
                    handoverTo = handoverTo,
                    checkPermission = false
                )
            )
        }
    }

    override fun batchHandoverApplicationFromPersonal(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): String {
        logger.info("batch handover group members from personal $userId|$projectCode|$handoverMemberDTO")
        handoverMemberDTO.checkHandoverTo()
        // 成员直接加入的组
        val groupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = handoverMemberDTO,
            minExpiredAt = LocalDateTime.now().timestampmilli()
        )[MemberType.get(MemberType.USER.type)]?.toMutableList()
        if (groupIds.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GROUP_NOT_EXIST
            )
        }

        // 过滤掉审核中的用户组
        val beingHandoverGroups = permissionHandoverApplicationService.listMemberHandoverDetails(
            projectCode = projectCode,
            memberId = handoverMemberDTO.targetMember.id,
            handoverType = HandoverType.GROUP
        ).map { it.itemId.toInt() }
        groupIds.removeAll(beingHandoverGroups)
        // 本次操作导致失效的授权
        val invalidAuthorizations = listInvalidAuthorizationsAfterOperatedGroups(
            projectCode = projectCode,
            iamGroupIdsOfDirectlyJoined = groupIds,
            memberId = handoverMemberDTO.targetMember.id
        )

        val invalidPipelines = invalidAuthorizations.invalidPipelineIds
        val invalidRepertoryIds = invalidAuthorizations.invalidRepertoryIds
        val invalidEnvNodeIds = invalidAuthorizations.invalidEnvNodeIds
        if (invalidRepertoryIds.isNotEmpty()) {
            permissionAuthorizationService.checkRepertoryAuthorizationsHanover(
                operator = userId,
                projectCode = projectCode,
                repertoryIds = invalidRepertoryIds,
                handoverFrom = handoverMemberDTO.targetMember.id,
                handoverTo = handoverMemberDTO.handoverTo.id
            )
        }
        val handoverDetails = buildHandoverDetails(
            projectCode = projectCode,
            groupIds = groupIds.map { it.toString() },
            pipelineAuthorizations = invalidPipelines,
            repertoryAuthorizations = invalidRepertoryIds,
            envNodeAuthorizations = invalidEnvNodeIds
        )
        val projectName = authResourceService.get(
            projectCode = projectCode,
            resourceType = ResourceTypeId.PROJECT,
            resourceCode = projectCode
        ).resourceName
        // 创建交接单
        val flowNo = permissionHandoverApplicationService.createHandoverApplication(
            overview = HandoverOverviewCreateDTO(
                projectCode = projectCode,
                projectName = projectName,
                applicant = handoverMemberDTO.targetMember.id,
                approver = handoverMemberDTO.handoverTo.id,
                handoverStatus = HandoverStatus.PENDING,
                groupCount = groupIds.size,
                authorizationCount = invalidPipelines.size + invalidRepertoryIds.size
            ),
            details = handoverDetails
        )
        return flowNo
    }

    private fun buildHandoverDetails(
        projectCode: String,
        groupIds: List<String>,
        pipelineAuthorizations: List<String>,
        repertoryAuthorizations: List<String>,
        envNodeAuthorizations: List<String>
    ): List<HandoverDetailDTO> {
        val handoverDetails = mutableListOf<HandoverDetailDTO>()
        if (groupIds.isNotEmpty()) {
            val resourceGroups = authResourceGroupDao.listByRelationId(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = groupIds
            )
            resourceGroups.forEach { groupInfo ->
                handoverDetails.add(
                    HandoverDetailDTO(
                        projectCode = projectCode,
                        itemId = groupInfo.relationId,
                        resourceType = groupInfo.resourceType,
                        handoverType = HandoverType.GROUP
                    )
                )
            }
        }

        pipelineAuthorizations.forEach { pipelineId ->
            handoverDetails.add(
                HandoverDetailDTO(
                    projectCode = projectCode,
                    itemId = pipelineId,
                    resourceType = ResourceTypeId.PIPELINE,
                    handoverType = HandoverType.AUTHORIZATION
                )
            )
        }
        repertoryAuthorizations.forEach { repertoryId ->
            handoverDetails.add(
                HandoverDetailDTO(
                    projectCode = projectCode,
                    itemId = repertoryId,
                    resourceType = ResourceTypeId.REPERTORY,
                    handoverType = HandoverType.AUTHORIZATION
                )
            )
        }
        envNodeAuthorizations.forEach { envNodeId ->
            handoverDetails.add(
                HandoverDetailDTO(
                    projectCode = projectCode,
                    itemId = envNodeId,
                    resourceType = ResourceTypeId.ENV_NODE,
                    handoverType = HandoverType.AUTHORIZATION
                )
            )
        }
        return handoverDetails
    }

    override fun batchDeleteResourceGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Boolean {
        logger.info("batch delete group members $userId|$projectCode|$removeMemberDTO")
        // 若操作对象是组织，则直接退出即可。
        if (removeMemberDTO.targetMember.type == MemberType.DEPARTMENT.type) {
            batchOperateGroupMembers(
                projectCode = projectCode,
                type = BatchOperateType.REMOVE,
                conditionReq = removeMemberDTO,
                operateGroupMemberTask = ::deleteTask
            )
            return true
        }
        // 以下逻辑是用户类型成员的批量移出组
        // 根据条件获取成员直接加入的用户组
        val groupIdsDirectlyJoined = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = removeMemberDTO
        )[MemberType.USER] ?: return true

        val invalidAuthorizationsDTO = listInvalidAuthorizationsAfterOperatedGroups(
            projectCode = projectCode,
            iamGroupIdsOfDirectlyJoined = groupIdsDirectlyJoined,
            memberId = removeMemberDTO.targetMember.id
        )
        val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) = invalidAuthorizationsDTO

        if (invalidRepertoryIds.isNotEmpty()) {
            permissionAuthorizationService.checkRepertoryAuthorizationsHanover(
                operator = userId,
                projectCode = projectCode,
                repertoryIds = invalidRepertoryIds,
                handoverFrom = removeMemberDTO.targetMember.id,
                handoverTo = removeMemberDTO.handoverTo!!.id
            )
        }
        // 获取唯一管理员组
        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = groupIdsDirectlyJoined
        )
        val (toHandoverGroups, toDeleteGroups) = groupIdsDirectlyJoined.partition {
            uniqueManagerGroups.contains(it) || invalidGroups.contains(it)
        }
        // 直接退出的用户组
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.REMOVE,
            conditionReq = GroupMemberRemoveConditionReq(
                groupIds = toDeleteGroups.map {
                    MemberGroupJoinedDTO(
                        id = it,
                        memberType = MemberType.USER
                    )
                },
                targetMember = removeMemberDTO.targetMember
            ),
            operateGroupMemberTask = ::deleteTask
        )
        // 交接唯一拥有者、影响代持人权限的用户组以及流水线/代码库授权/环境节点授权
        if (toHandoverGroups.isNotEmpty()) {
            removeMemberDTO.checkHandoverTo()
            batchOperateGroupMembers(
                projectCode = projectCode,
                type = BatchOperateType.HANDOVER,
                conditionReq = GroupMemberHandoverConditionReq(
                    groupIds = toHandoverGroups.map {
                        MemberGroupJoinedDTO(
                            id = it,
                            memberType = MemberType.USER
                        )
                    },
                    targetMember = removeMemberDTO.targetMember,
                    handoverTo = removeMemberDTO.handoverTo!!
                ),
                operateGroupMemberTask = ::handoverTask
            )
        }
        if (invalidAuthorizationsDTO.isHasInvalidAuthorizations()) {
            handoverAuthorizationsWhenOperatedGroups(
                userId = userId,
                projectCode = projectCode,
                invalidPipelines = invalidPipelines,
                invalidRepertoryIds = invalidRepertoryIds,
                invalidEnvNodeIds = invalidEnvNodeIds,
                handoverFrom = removeMemberDTO.targetMember.id,
                handoverTo = removeMemberDTO.handoverTo!!.id
            )
        }
        return true
    }

    override fun batchDeleteResourceGroupMembersFromPersonal(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): String {
        logger.info("batch delete group members from personal $userId|$projectCode|$removeMemberDTO")
        // 根据条件获取成员直接加入的用户组
        val groupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = removeMemberDTO
        )[MemberType.USER]?.toMutableList() ?: return "true"

        // 过滤掉审核中的用户组
        val beingHandoverGroups = permissionHandoverApplicationService.listMemberHandoverDetails(
            projectCode = projectCode,
            memberId = removeMemberDTO.targetMember.id,
            handoverType = HandoverType.GROUP
        ).map { it.itemId.toInt() }
        groupIds.removeAll(beingHandoverGroups)

        val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) =
            listInvalidAuthorizationsAfterOperatedGroups(
                projectCode = projectCode,
                iamGroupIdsOfDirectlyJoined = groupIds,
                memberId = removeMemberDTO.targetMember.id
            )

        // 检查授予人是否有代码库oauth权限
        if (invalidRepertoryIds.isNotEmpty()) {
            permissionAuthorizationService.checkRepertoryAuthorizationsHanover(
                operator = userId,
                projectCode = projectCode,
                repertoryIds = invalidRepertoryIds,
                handoverFrom = removeMemberDTO.targetMember.id,
                handoverTo = removeMemberDTO.handoverTo!!.id
            )
        }

        // 获取唯一管理员组
        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = groupIds
        )
        val (toHandoverGroups, toDeleteGroups) = groupIds.partition {
            uniqueManagerGroups.contains(it) || invalidGroups.contains(it)
        }
        // 直接退出的用户组
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.REMOVE,
            conditionReq = GroupMemberRemoveConditionReq(
                groupIds = toDeleteGroups.map {
                    MemberGroupJoinedDTO(
                        id = it,
                        memberType = MemberType.USER
                    )
                },
                targetMember = removeMemberDTO.targetMember
            ),
            operateGroupMemberTask = ::deleteTask
        )
        if (toHandoverGroups.isEmpty() && invalidPipelines.isEmpty() && invalidRepertoryIds.isEmpty() &&
            invalidEnvNodeIds.isEmpty()) {
            return "true"
        }
        val handoverDetails = buildHandoverDetails(
            projectCode = projectCode,
            groupIds = toHandoverGroups.map { it.toString() },
            pipelineAuthorizations = invalidPipelines,
            repertoryAuthorizations = invalidRepertoryIds,
            envNodeAuthorizations = invalidEnvNodeIds
        )

        val projectName = authResourceService.get(
            projectCode = projectCode,
            resourceType = ResourceTypeId.PROJECT,
            resourceCode = projectCode
        ).resourceName
        val flowNo = permissionHandoverApplicationService.createHandoverApplication(
            overview = HandoverOverviewCreateDTO(
                projectCode = projectCode,
                projectName = projectName,
                applicant = removeMemberDTO.targetMember.id,
                approver = removeMemberDTO.handoverTo!!.id,
                handoverStatus = HandoverStatus.PENDING,
                groupCount = toHandoverGroups.size,
                authorizationCount = invalidPipelines.size + invalidRepertoryIds.size
            ),
            details = handoverDetails
        )
        return flowNo
    }

    override fun deleteResourceGroupMembers(
        userId: String,
        projectCode: String,
        groupId: Int,
        targetMember: ResourceMemberInfo
    ): Boolean {
        logger.info("delete single group members from personal:$userId|$targetMember|$projectCode|$groupId")
        if (targetMember.type == MemberType.USER.type) {
            val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) =
                listInvalidAuthorizationsAfterOperatedGroups(
                    projectCode = projectCode,
                    iamGroupIdsOfDirectlyJoined = listOf(groupId),
                    memberId = targetMember.id
                )
            if (invalidGroups.isNotEmpty() || invalidPipelines.isNotEmpty() ||
                invalidRepertoryIds.isNotEmpty() || invalidEnvNodeIds.isNotEmpty()) {
                throw ErrorCodeException(errorCode = ERROR_SINGLE_GROUP_REMOVE)
            }
        }
        batchOperateGroupMembers(
            projectCode = projectCode,
            type = BatchOperateType.REMOVE,
            conditionReq = GroupMemberRemoveConditionReq(
                groupIds = listOf(
                    MemberGroupJoinedDTO(
                        id = groupId,
                        memberType = MemberType.get(targetMember.type)
                    )
                ),
                targetMember = targetMember
            ),
            operateGroupMemberTask = ::deleteTask
        )
        return true
    }

    private fun handoverTask(
        projectCode: String,
        groupId: Int,
        handoverMemberDTO: GroupMemberHandoverConditionReq,
        expiredAt: Long
    ) {
        logger.info(
            "handover group member $projectCode|$groupId|" +
                "${handoverMemberDTO.targetMember}|${handoverMemberDTO.handoverTo}"
        )
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        var finalExpiredAt = expiredAt
        // 若交接人的权限已过期，如果是唯一管理员组，允许交接，接收人将获得半年权限；其他的直接删除。
        if (expiredAt < currentTimeSeconds) {
            val isUniqueManagerGroup = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = listOf(groupId)
            ).isNotEmpty()
            if (isUniqueManagerGroup) {
                finalExpiredAt = currentTimeSeconds + TimeUnit.DAYS.toSeconds(180)
            } else {
                deleteTask(
                    projectCode = projectCode,
                    groupId = groupId,
                    removeMemberDTO = GroupMemberRemoveConditionReq(
                        targetMember = handoverMemberDTO.targetMember
                    ),
                    expiredAt = finalExpiredAt
                )
                return
            }
        }

        val isHandoverToInGroup = authResourceGroupMemberDao.isMemberInGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = groupId,
            memberId = handoverMemberDTO.handoverTo.id
        )
        if (isHandoverToInGroup) {
            deleteTask(
                projectCode = projectCode,
                groupId = groupId,
                removeMemberDTO = GroupMemberRemoveConditionReq(
                    targetMember = handoverMemberDTO.handoverTo
                ),
                expiredAt = finalExpiredAt
            )
        }
        if (finalExpiredAt < currentTimeSeconds) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_EXPIRED_PERM_NOT_ALLOW_TO_HANDOVER
            )
        }
        val members = listOf(
            ManagerMember(
                handoverMemberDTO.handoverTo.type,
                handoverMemberDTO.handoverTo.id
            )
        )
        permissionResourceMemberService.addIamGroupMember(
            groupId = groupId,
            members = members,
            expiredAt = finalExpiredAt
        )
        permissionResourceMemberService.deleteIamGroupMembers(
            groupId = groupId,
            type = handoverMemberDTO.targetMember.type,
            memberIds = listOf(handoverMemberDTO.targetMember.id)
        )
        authResourceGroupMemberDao.handoverGroupMembers(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = groupId,
            handoverFrom = handoverMemberDTO.targetMember,
            handoverTo = handoverMemberDTO.handoverTo,
            expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(finalExpiredAt)
        )
    }

    private fun deleteTask(
        projectCode: String,
        groupId: Int,
        removeMemberDTO: GroupMemberRemoveConditionReq,
        expiredAt: Long
    ) {
        val targetMember = removeMemberDTO.targetMember
        logger.info("delete group member $projectCode|$groupId|$targetMember")
        permissionResourceMemberService.deleteIamGroupMembers(
            groupId = groupId,
            type = targetMember.type,
            memberIds = listOf(targetMember.id)
        )
        authResourceGroupMemberDao.batchDeleteGroupMembers(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = groupId,
            memberIds = listOf(removeMemberDTO.targetMember.id)
        )
    }

    override fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): BatchOperateGroupMemberCheckVo {
        logger.info("batch operate group member check|$userId|$projectCode|$batchOperateType|$conditionReq")
        // 获取成员加入的用户组
        val joinedType2GroupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = conditionReq
        )
        // 通过组织或者模板加入的用户组
        val groupsOfTemplateOrDeptJoined = when (conditionReq.targetMember.type) {
            MemberType.USER.type -> {
                listOfNotNull(
                    joinedType2GroupIds[MemberType.DEPARTMENT],
                    joinedType2GroupIds[MemberType.TEMPLATE]
                ).flatten()
            }

            else -> joinedType2GroupIds[MemberType.TEMPLATE] ?: emptyList()
        }
        // 直接加入的组
        val groupsOfDirectlyJoined = joinedType2GroupIds[MemberType.get(conditionReq.targetMember.type)] ?: emptyList()
        // 总数
        val totalCount = groupsOfTemplateOrDeptJoined.size + groupsOfDirectlyJoined.size
        return when (batchOperateType) {
            BatchOperateType.REMOVE -> {
                if (conditionReq.targetMember.type == MemberType.DEPARTMENT.type) {
                    BatchOperateGroupMemberCheckVo(
                        totalCount = totalCount,
                        operableCount = groupsOfDirectlyJoined.size,
                        inoperableCount = groupsOfTemplateOrDeptJoined.size
                    )
                } else {
                    val groupsOfUniqueManager = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        iamGroupIds = groupsOfDirectlyJoined
                    )
                    // 本次操作导致流水线代持人权限受到影响的用户组及流水线/代码库oauth/环境节点
                    val invalidAuthorizationsDTO = listInvalidAuthorizationsAfterOperatedGroups(
                        projectCode = projectCode,
                        iamGroupIdsOfDirectlyJoined = groupsOfDirectlyJoined,
                        memberId = conditionReq.targetMember.id
                    )
                    val (invalidGroups, invalidPipelines, invalidRepositoryIds, invalidEnvNodeIds) =
                        invalidAuthorizationsDTO

                    // 当批量移出时，
                    // 直接加入的组中，唯一管理员组/影响流水线代持权限不允许被移出
                    // 间接加入的组中，通过组织、模板加入的组不允许被移出
                    val groupsOfInOperableWhenBatchRemove = groupsOfDirectlyJoined.count {
                        groupsOfUniqueManager.contains(it) || invalidGroups.contains(it)
                    } + groupsOfTemplateOrDeptJoined.size
                    val canHandoverCount = groupsOfUniqueManager.union(invalidGroups).size
                    BatchOperateGroupMemberCheckVo(
                        totalCount = totalCount,
                        operableCount = totalCount - groupsOfInOperableWhenBatchRemove,
                        inoperableCount = groupsOfInOperableWhenBatchRemove,
                        uniqueManagerCount = groupsOfUniqueManager.size,
                        invalidGroupCount = invalidGroups.size,
                        invalidPipelineAuthorizationCount = invalidPipelines.size,
                        invalidRepositoryAuthorizationCount = invalidRepositoryIds.size,
                        invalidEnvNodeAuthorizationCount = invalidEnvNodeIds.size,
                        canHandoverCount = canHandoverCount,
                        needToHandover = invalidAuthorizationsDTO.isHasInvalidAuthorizations() || canHandoverCount > 0
                    )
                }
            }

            BatchOperateType.RENEWAL -> {
                // 部门/组织加入以及永久权限的组不允许再续期
                with(conditionReq) {
                    val isUserDeparted = targetMember.type == MemberType.USER.type &&
                        deptService.isUserDeparted(targetMember.id)
                    // 离职用户不允许续期
                    if (isUserDeparted) {
                        BatchOperateGroupMemberCheckVo(
                            totalCount = totalCount,
                            inoperableCount = totalCount
                        )
                    } else {
                        // 永久期限 不允许再续期
                        val groupCountOfPermanentExpiredTime = listMemberGroupsDetails(
                            projectCode = projectCode,
                            memberId = targetMember.id,
                            memberType = targetMember.type,
                            groupIds = groupsOfDirectlyJoined
                        ).filter {
                            // iam用的是秒级时间戳
                            it.expiredAt == PERMANENT_EXPIRED_TIME / 1000
                        }.size
                        val groupsOfInOperableWhenBatchRenewal = groupCountOfPermanentExpiredTime +
                            groupsOfTemplateOrDeptJoined.size
                        BatchOperateGroupMemberCheckVo(
                            totalCount = totalCount,
                            operableCount = totalCount - groupsOfInOperableWhenBatchRenewal,
                            inoperableCount = groupsOfInOperableWhenBatchRenewal
                        )
                    }
                }
            }

            BatchOperateType.HANDOVER -> {
                // 已过期（除唯一管理员组 ）或通过模板/组织加入的不允许移交
                with(conditionReq) {
                    val finalGroupIds = groupsOfDirectlyJoined.toMutableList()
                    val uniqueManagerGroupIds = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        iamGroupIds = groupsOfDirectlyJoined
                    )
                    // 去除唯一管理员组
                    if (uniqueManagerGroupIds.isNotEmpty()) {
                        finalGroupIds.removeAll(uniqueManagerGroupIds)
                    }
                    val groupCountOfExpired = listMemberGroupsDetails(
                        projectCode = projectCode,
                        memberId = targetMember.id,
                        memberType = targetMember.type,
                        groupIds = finalGroupIds
                    ).filter {
                        // iam用的是秒级时间戳
                        it.expiredAt < System.currentTimeMillis() / 1000
                    }.size
                    val inoperableCount = groupsOfTemplateOrDeptJoined.size + groupCountOfExpired
                    // 本次操作导致流水线代持人权限受到影响的流水线
                    val (invalidGroups, invalidPipelines, invalidRepositoryIds, invalidEnvNodeIds) =
                        listInvalidAuthorizationsAfterOperatedGroups(
                            projectCode = projectCode,
                            iamGroupIdsOfDirectlyJoined = groupsOfDirectlyJoined,
                            memberId = conditionReq.targetMember.id
                        )

                    BatchOperateGroupMemberCheckVo(
                        totalCount = totalCount,
                        operableCount = totalCount - inoperableCount,
                        inoperableCount = groupsOfTemplateOrDeptJoined.size + groupCountOfExpired,
                        invalidPipelineAuthorizationCount = invalidPipelines.size,
                        invalidRepositoryAuthorizationCount = invalidRepositoryIds.size,
                        invalidEnvNodeAuthorizationCount = invalidEnvNodeIds.size,
                        canHandoverCount = totalCount - inoperableCount
                    )
                }
            }

            else -> {
                BatchOperateGroupMemberCheckVo(
                    totalCount = totalCount,
                    inoperableCount = groupsOfTemplateOrDeptJoined.size,
                    operableCount = groupsOfDirectlyJoined.size
                )
            }
        }
    }

    override fun removeMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): List<ResourceMemberInfo> {
        logger.info("remove member from project $userId|$projectCode|$removeMemberFromProjectReq")
        return with(removeMemberFromProjectReq) {
            val memberType = targetMember.type
            if (memberType == MemberType.USER.type && isNeedToHandover()) {
                removeMemberFromProjectReq.checkHandoverTo()
                val handoverMemberDTO = GroupMemberHandoverConditionReq(
                    allSelection = true,
                    targetMember = targetMember,
                    handoverTo = handoverTo!!
                )
                batchOperateGroupMembers(
                    projectCode = projectCode,
                    type = BatchOperateType.HANDOVER,
                    conditionReq = handoverMemberDTO,
                    operateGroupMemberTask = ::handoverTask
                )
                permissionAuthorizationService.resetAllResourceAuthorization(
                    operator = userId,
                    projectCode = projectCode,
                    condition = ResetAllResourceAuthorizationReq(
                        projectCode = projectCode,
                        handoverFrom = removeMemberFromProjectReq.targetMember.id,
                        handoverTo = removeMemberFromProjectReq.handoverTo!!.id,
                        preCheck = false,
                        checkPermission = false
                    )
                )
            } else {
                val removeMemberDTO = GroupMemberRemoveConditionReq(
                    allSelection = true,
                    targetMember = targetMember
                )
                batchOperateGroupMembers(
                    projectCode = projectCode,
                    type = BatchOperateType.REMOVE,
                    conditionReq = removeMemberDTO,
                    operateGroupMemberTask = ::deleteTask
                )
            }

            if (memberType == MemberType.USER.type) {
                // 查询用户还存在那些组织中
                val userDeptInfos = deptService.getUserInfo(
                    userId = "admin",
                    name = targetMember.id
                )?.deptInfo?.map { it.name!! }
                if (userDeptInfos != null) {
                    return authResourceGroupMemberDao.isMembersInProject(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        memberNames = userDeptInfos,
                        memberType = MemberType.DEPARTMENT.type
                    )
                }
            }
            return emptyList()
        }
    }

    override fun removeMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Boolean {
        val targetMember = removeMemberFromProjectReq.targetMember
        val isMemberHasNoPermission = batchOperateGroupMembersCheck(
            userId = userId,
            projectCode = projectCode,
            batchOperateType = BatchOperateType.HANDOVER,
            conditionReq = GroupMemberCommonConditionReq(
                allSelection = true,
                targetMember = removeMemberFromProjectReq.targetMember
            )
        ).let { it.totalCount == it.inoperableCount }

        val isMemberHasNoAuthorizations =
            if (targetMember.type == MemberType.USER.type) {
                permissionAuthorizationService.listResourceAuthorizations(
                    condition = ResourceAuthorizationConditionRequest(
                        projectCode = projectCode,
                        handoverFrom = targetMember.id
                    )
                ).count == 0L
            } else {
                true
            }
        return isMemberHasNoPermission && isMemberHasNoAuthorizations
    }

    override fun handleHanoverApplication(request: HandoverOverviewUpdateReq): Boolean {
        val overview = permissionHandoverApplicationService.getHandoverOverview(request.flowNo)
        logger.info("handle hanover application:{}|{} ", request, overview)
        HandleHandoverApplicationLock(redisOperation, request.flowNo).use { lock ->
            if (!lock.tryLock()) {
                logger.warn("The handover application is being processed!$request")
                throw ErrorCodeException(errorCode = ERROR_HANDOVER_HANDLE)
            }
            try {
                handleHanoverCheck(request = request, overview = overview)
                if (request.handoverAction == HandoverAction.AGREE) {
                    handleHandoverAgreeAction(
                        request = request,
                        overview = overview
                    )
                }
                permissionHandoverApplicationService.updateHandoverApplication(
                    overview = request
                )
                val projectName = authResourceService.get(
                    projectCode = request.projectCode,
                    resourceType = ResourceTypeId.PROJECT,
                    resourceCode = request.projectCode
                ).resourceName
                val handoverFromCnName = deptService.getMemberInfo(
                    overview.applicant, ManagerScopesEnum.USER
                ).displayName
                val handoverToCnName = deptService.getMemberInfo(
                    overview.approver, ManagerScopesEnum.USER
                ).displayName
                val bodyParams = mapOf(
                    "projectName" to projectName,
                    "result" to request.handoverAction.alias,
                    "handoverFrom" to overview.applicant.plus("($handoverFromCnName)"),
                    "remark" to request.remark!!,
                    "content" to String.format(
                        request.handoverAction.emailContent,
                        request.flowNo,
                        overview.approver.plus("($handoverToCnName)")
                    ),
                    "weworkContent" to String.format(
                        request.handoverAction.weworkContent,
                        request.flowNo,
                        overview.approver.plus("($handoverToCnName)"),
                    ),
                    "url" to String.format(url, request.flowNo)
                )
                // 发邮件
                val emailRequest = SendNotifyMessageTemplateRequest(
                    templateCode = HANDOVER_APPLICATION_RESULT_TEMPLATE_CODE,
                    bodyParams = bodyParams,
                    titleParams = bodyParams,
                    notifyType = mutableSetOf(NotifyType.WEWORK.name, NotifyType.EMAIL.name),
                    receivers = mutableSetOf(overview.applicant)
                )
                logger.info("send handover application result email:{}|{} ", request, emailRequest)
                kotlin.runCatching {
                    client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(emailRequest)
                }.onFailure {
                    logger.warn("notify email fail ${it.message}|$bodyParams|${overview.approver}")
                }
            } catch (e: Exception) {
                logger.warn("handle hanover application error,$e|$request")
                throw e
            }
        }
        return true
    }

    override fun batchHandleHanoverApplications(request: HandoverOverviewBatchUpdateReq): Boolean {
        logger.info("batch handle hanover application:{} ", request)
        val startEpoch = System.currentTimeMillis()
        try {
            val overviews = when {
                request.allSelection -> permissionHandoverApplicationService.listHandoverOverviews(
                    queryRequest = HandoverOverviewQueryReq(
                        memberId = request.operator,
                        approver = request.operator,
                        handoverStatus = HandoverStatus.PENDING
                    )
                )

                request.flowNos.isNotEmpty() -> permissionHandoverApplicationService.listHandoverOverviews(
                    queryRequest = HandoverOverviewQueryReq(
                        memberId = request.operator,
                        approver = request.operator,
                        handoverStatus = HandoverStatus.PENDING,
                        flowNos = request.flowNos
                    )
                )

                else -> return true
            }.records
            overviews.forEach { overview ->
                handleHanoverApplication(
                    request = HandoverOverviewUpdateReq(
                        projectCode = overview.projectCode,
                        flowNo = overview.flowNo,
                        operator = request.operator,
                        handoverAction = request.handoverAction,
                        remark = request.remark
                    )
                )
            }
        } finally {
            "It take(${System.currentTimeMillis() - startEpoch})ms to batch handle hanover applications"
        }
        return true
    }

    override fun getResourceType2CountOfHandover(
        queryReq: ResourceType2CountOfHandoverQuery
    ): List<ResourceType2CountVo> {
        queryReq.check()
        return if (queryReq.queryChannel == HandoverQueryChannel.HANDOVER_APPLICATION) {
            permissionHandoverApplicationService.getResourceType2CountOfHandoverApplication(queryReq.flowNo!!)
        } else {
            getResourceType2CountOfHandoverPreview(queryReq)
        }
    }

    // 交接预览
    private fun getResourceType2CountOfHandoverPreview(
        queryReq: ResourceType2CountOfHandoverQuery
    ): List<ResourceType2CountVo> {
        val projectCode = queryReq.projectCode
        val previewConditionReq = queryReq.previewConditionReq!!
        val batchOperateType = queryReq.batchOperateType!!
        val groupIdsDirectlyJoined = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = previewConditionReq
        )[MemberType.USER] ?: return emptyList()

        val result = mutableListOf<ResourceType2CountVo>()
        val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) =
            listInvalidAuthorizationsAfterOperatedGroups(
                projectCode = projectCode,
                iamGroupIdsOfDirectlyJoined = groupIdsDirectlyJoined,
                memberId = previewConditionReq.targetMember.id
            )
        if (batchOperateType == BatchOperateType.REMOVE) {
            // 只有一个成员的管理员组
            val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = groupIdsDirectlyJoined
            )
            val needToHandoverGroupIds = invalidGroups.union(uniqueManagerGroups).map { it.toString() }
            val resourceType2CountOfGroup = authResourceGroupDao.getResourceType2Count(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = needToHandoverGroupIds
            )
            if (resourceType2CountOfGroup.isNotEmpty()) {
                result.addAll(
                    rbacCommonService.convertResourceType2Count(
                        resourceType2Count = resourceType2CountOfGroup,
                        type = HandoverType.GROUP
                    )
                )
            }
        }
        if (invalidPipelines.isNotEmpty()) {
            result.addAll(
                rbacCommonService.convertResourceType2Count(
                    resourceType2Count = mapOf(ResourceTypeId.PIPELINE to invalidPipelines.size.toLong()),
                    type = HandoverType.AUTHORIZATION
                )
            )
        }
        if (invalidRepertoryIds.isNotEmpty()) {
            result.addAll(
                rbacCommonService.convertResourceType2Count(
                    resourceType2Count = mapOf(ResourceTypeId.REPERTORY to invalidRepertoryIds.size.toLong()),
                    type = HandoverType.AUTHORIZATION
                )
            )
        }
        if (invalidEnvNodeIds.isNotEmpty()) {
            result.addAll(
                rbacCommonService.convertResourceType2Count(
                    resourceType2Count = mapOf(ResourceTypeId.ENV_NODE to invalidEnvNodeIds.size.toLong()),
                    type = HandoverType.AUTHORIZATION
                )
            )
        }
        return result
    }

    override fun listAuthorizationsOfHandover(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo> {
        queryReq.check()
        return if (queryReq.queryChannel == HandoverQueryChannel.HANDOVER_APPLICATION) {
            permissionHandoverApplicationService.listAuthorizationsOfHandoverApplication(queryReq)
        } else {
            listAuthorizationsOfHandoverPreview(queryReq)
        }
    }

    private fun listAuthorizationsOfHandoverPreview(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo> {
        val projectCode = queryReq.projectCode
        val previewConditionReq = queryReq.previewConditionReq!!
        val groupIdsDirectlyJoined = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = previewConditionReq
        )[MemberType.USER] ?: return SQLPage(0, emptyList())
        val (invalidGroups, invalidPipelines, invalidRepertoryIds, invalidEnvNodeIds) =
            listInvalidAuthorizationsAfterOperatedGroups(
                projectCode = projectCode,
                iamGroupIdsOfDirectlyJoined = groupIdsDirectlyJoined,
                memberId = previewConditionReq.targetMember.id
            )

        val invalidResources = when (queryReq.resourceType) {
            ResourceTypeId.PIPELINE -> invalidPipelines
            ResourceTypeId.ENV_NODE -> invalidEnvNodeIds
            else -> invalidRepertoryIds
        }

        val records = authorizationDao.list(
            dslContext = dslContext,
            condition = ResourceAuthorizationConditionRequest(
                projectCode = projectCode,
                resourceType = queryReq.resourceType,
                filterResourceCodes = invalidResources,
                page = queryReq.page,
                pageSize = queryReq.pageSize
            )
        ).map {
            HandoverAuthorizationDetailVo(
                resourceCode = it.resourceCode,
                resourceName = it.resourceName,
                handoverType = HandoverType.AUTHORIZATION,
                handoverFrom = it.handoverFrom
            )
        }
        return SQLPage(count = invalidResources.size.toLong(), records = records)
    }

    override fun listGroupsOfHandover(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo> {
        queryReq.check()
        return if (queryReq.queryChannel == HandoverQueryChannel.HANDOVER_APPLICATION) {
            permissionHandoverApplicationService.listGroupsOfHandoverApplication(queryReq)
        } else {
            listGroupsOfHandoverPreview(queryReq)
        }
    }

    override fun isProjectMember(
        projectCode: String,
        userId: String
    ): Boolean {
        // 获取用户加入的项目级用户组模板ID
        val iamTemplateIds = listProjectMemberGroupTemplateIds(
            projectCode = projectCode,
            memberId = userId
        )
        val memberDeptInfos = deptService.getUserInfo(
            userId = "admin",
            name = userId
        )?.deptInfo?.map { it.name!! }

        return authResourceGroupMemberDao.isMemberInProject(
            dslContext = dslContext,
            projectCode = projectCode,
            userId = userId,
            iamTemplateIds = iamTemplateIds,
            memberDeptInfos = memberDeptInfos
        ) || rbacCommonService.validateUserProjectPermission(
            userId = userId,
            projectCode = projectCode,
            permission = AuthPermission.VISIT
        )
    }

    override fun checkMemberExitsProject(
        projectCode: String,
        userId: String
    ): MemberExitsProjectCheckVo {
        logger.info("check member exits project:$projectCode|$userId")
        val userDeptInfos = deptService.getUserInfo(
            userId = "admin",
            name = userId
        )?.deptInfo?.map { it.name!! } ?: emptyList()
        val userDepartmentsInProject = authResourceGroupMemberDao.isMembersInProject(
            dslContext = dslContext,
            projectCode = projectCode,
            memberNames = userDeptInfos,
            memberType = MemberType.DEPARTMENT.type
        ).map { it.name }
        if (userDepartmentsInProject.isNotEmpty()) {
            val managers = permissionResourceMemberService.getResourceGroupMembers(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                group = BkAuthGroup.MANAGER
            )
            return MemberExitsProjectCheckVo(
                departmentJoinedCount = userDepartmentsInProject.size,
                departments = userDepartmentsInProject.joinToString(","),
                managers = managers
            )
        }
        val resourceType2Authorizations = authAuthorizationDao.list(
            dslContext = dslContext,
            condition = ResourceAuthorizationConditionRequest(
                projectCode = projectCode,
                handoverFrom = userId
            )
        ).groupBy { it.resourceType }
        val groupIdsDirectlyJoined = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = GroupMemberCommonConditionReq(
                allSelection = true,
                targetMember = ResourceMemberInfo(
                    id = userId,
                    type = MemberType.USER.type
                )
            )
        )[MemberType.USER] ?: emptyList()
        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = groupIdsDirectlyJoined
        )
        return MemberExitsProjectCheckVo(
            uniqueManagerCount = uniqueManagerGroups.size,
            pipelineAuthorizationCount = resourceType2Authorizations[ResourceTypeId.PIPELINE]?.size ?: 0,
            repositoryAuthorizationCount = resourceType2Authorizations[ResourceTypeId.REPERTORY]?.size ?: 0,
            envNodeAuthorizationCount = resourceType2Authorizations[ResourceTypeId.ENV_NODE]?.size ?: 0
        )
    }

    override fun memberExitsProject(
        projectCode: String,
        request: RemoveMemberFromProjectReq
    ): String {
        logger.info("member exits project :$projectCode|$request")
        if (request.isNeedToHandover()) {
            request.checkHandoverTo()
            val handoverTo = request.handoverTo!!
            // 需要交接的用户组
            val groupIds = getGroupIdsByGroupMemberCondition(
                projectCode = projectCode,
                commonCondition = GroupMemberCommonConditionReq(
                    targetMember = request.targetMember,
                    allSelection = true
                )
            )[MemberType.get(MemberType.USER.type)]?.toMutableList() ?: emptyList()
            // 需要交接的授权管理
            val resourceAuthorizations = authAuthorizationDao.list(
                dslContext = dslContext,
                condition = ResourceAuthorizationConditionRequest(
                    projectCode = projectCode,
                    handoverFrom = request.targetMember.id
                )
            )
            val resourceType2Authorizations = resourceAuthorizations.groupBy({ it.resourceType }, { it.resourceCode })
            val repertoryAuthorizations = resourceType2Authorizations[ResourceTypeId.REPERTORY] ?: emptyList()
            val pipelineAuthorizations = resourceType2Authorizations[ResourceTypeId.PIPELINE] ?: emptyList()
            val envNodeRepertoryIds = resourceType2Authorizations[ResourceTypeId.ENV_NODE] ?: emptyList()
            if (repertoryAuthorizations.isNotEmpty()) {
                permissionAuthorizationService.checkRepertoryAuthorizationsHanover(
                    operator = request.targetMember.id,
                    projectCode = projectCode,
                    repertoryIds = repertoryAuthorizations,
                    handoverFrom = request.targetMember.id,
                    handoverTo = handoverTo.id
                )
            }
            val handoverDetails = buildHandoverDetails(
                projectCode = projectCode,
                groupIds = groupIds.map { it.toString() },
                pipelineAuthorizations = pipelineAuthorizations,
                repertoryAuthorizations = repertoryAuthorizations,
                envNodeAuthorizations = envNodeRepertoryIds
            )
            val projectName = authResourceService.get(
                projectCode = projectCode,
                resourceType = ResourceTypeId.PROJECT,
                resourceCode = projectCode
            ).resourceName
            // 创建交接单
            val flowNo = permissionHandoverApplicationService.createHandoverApplication(
                overview = HandoverOverviewCreateDTO(
                    projectCode = projectCode,
                    projectName = projectName,
                    applicant = request.targetMember.id,
                    approver = handoverTo.id,
                    handoverStatus = HandoverStatus.PENDING,
                    groupCount = groupIds.size,
                    authorizationCount = resourceAuthorizations.size
                ),
                details = handoverDetails
            )
            return flowNo
        } else {
            val result = checkMemberExitsProject(
                projectCode = projectCode,
                userId = request.targetMember.id
            )
            if (!result.canExitsProjectDirectly()) {
                throw OperationException(
                    message = "Direct exit from the project is not allowed！"
                )
            }
            val removeMemberDTO = GroupMemberRemoveConditionReq(
                allSelection = true,
                targetMember = request.targetMember
            )
            batchOperateGroupMembers(
                projectCode = projectCode,
                type = BatchOperateType.REMOVE,
                conditionReq = removeMemberDTO,
                operateGroupMemberTask = ::deleteTask
            )
        }
        return ""
    }

    private fun listGroupsOfHandoverPreview(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo> {
        val projectCode = queryReq.projectCode
        val previewConditionReq = queryReq.previewConditionReq!!
        val convertPageSizeToSQLLimit = PageUtil.convertPageSizeToSQLLimit(queryReq.page, queryReq.pageSize)
        val groupIdsDirectlyJoined = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = previewConditionReq
        )[MemberType.USER] ?: return SQLPage(0, emptyList())
        val invalidGroupIds = listInvalidAuthorizationsAfterOperatedGroups(
            projectCode = projectCode,
            iamGroupIdsOfDirectlyJoined = groupIdsDirectlyJoined,
            memberId = previewConditionReq.targetMember.id
        ).invalidGroupIds
        // 只有一个成员的管理员组
        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = groupIdsDirectlyJoined
        )
        val needToHandoverGroupIds = invalidGroupIds.union(uniqueManagerGroups).map { it.toString() }
        val records = authResourceGroupDao.listGroupByResourceType(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = queryReq.resourceType,
            iamGroupIds = needToHandoverGroupIds,
            offset = convertPageSizeToSQLLimit.offset,
            limit = convertPageSizeToSQLLimit.limit
        ).map {
            HandoverGroupDetailVo(
                projectCode = it.projectCode,
                iamGroupId = it.relationId,
                groupName = it.groupName,
                groupDesc = it.description,
                resourceCode = it.resourceCode,
                resourceName = it.resourceName
            )
        }
        return SQLPage(count = invalidGroupIds.size.toLong(), records = records)
    }

    private fun handleHanoverCheck(
        request: HandoverOverviewUpdateReq,
        overview: HandoverOverviewVo
    ) {
        if (overview.handoverStatus != HandoverStatus.PENDING) {
            throw ErrorCodeException(errorCode = ERROR_HANDOVER_FINISH)
        }
        if (request.handoverAction == HandoverAction.REVOKE && request.operator != overview.applicant) {
            throw ErrorCodeException(errorCode = ERROR_HANDOVER_REVOKE)
        }
        if (request.handoverAction != HandoverAction.REVOKE && request.operator != overview.approver) {
            throw ErrorCodeException(errorCode = ERROR_HANDOVER_APPROVAL)
        }
    }

    private fun handleHandoverAgreeAction(
        request: HandoverOverviewUpdateReq,
        overview: HandoverOverviewVo
    ) {
        val handoverDetails = permissionHandoverApplicationService.listHandoverDetails(
            projectCode = overview.projectCode,
            flowNo = overview.flowNo
        )
        val handoverType2Records = handoverDetails.groupBy { it.handoverType }

        // 交接用户组
        val groupsOfHandover = handoverType2Records[HandoverType.GROUP]?.map { it.itemId.toInt() }
        if (!groupsOfHandover.isNullOrEmpty()) {
            val targetMember = ResourceMemberInfo(
                id = overview.applicant,
                name = deptService.getMemberInfo(overview.applicant, ManagerScopesEnum.USER).displayName,
                type = MemberType.USER.type
            )
            val handoverTo = ResourceMemberInfo(
                id = overview.approver,
                name = deptService.getMemberInfo(overview.approver, ManagerScopesEnum.USER).displayName,
                type = MemberType.USER.type
            )

            val groupMemberHandoverConditionReq = GroupMemberHandoverConditionReq(
                groupIds = groupsOfHandover.map {
                    MemberGroupJoinedDTO(
                        id = it,
                        memberType = MemberType.USER
                    )
                },
                targetMember = targetMember,
                handoverTo = handoverTo
            )
            batchOperateGroupMembers(
                projectCode = overview.projectCode,
                type = BatchOperateType.HANDOVER,
                conditionReq = groupMemberHandoverConditionReq,
                operateGroupMemberTask = ::handoverTask
            )
        }

        // 交接授权
        val authorizationsOfHandover = handoverType2Records[HandoverType.AUTHORIZATION]
        if (!authorizationsOfHandover.isNullOrEmpty()) {
            val resourceType2Authorizations = authorizationsOfHandover.groupBy { it.resourceType }
            resourceType2Authorizations.forEach { (resourceType, authorizations) ->
                permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                    operator = request.operator,
                    projectCode = overview.projectCode,
                    condition = ResourceAuthorizationHandoverConditionRequest(
                        projectCode = overview.projectCode,
                        resourceType = resourceType,
                        filterResourceCodes = authorizations.map { it.itemId },
                        fullSelection = true,
                        handoverChannel = HandoverChannelCode.MANAGER,
                        handoverFrom = overview.applicant,
                        handoverTo = overview.approver,
                        checkPermission = false
                    )
                )
            }
        }
    }

    private fun <T : GroupMemberCommonConditionReq> batchOperateGroupMembers(
        projectCode: String,
        conditionReq: T,
        type: BatchOperateType,
        operateGroupMemberTask: (
            projectCode: String,
            groupId: Int,
            conditionReq: T,
            expiredAt: Long
        ) -> Unit
    ): Boolean {
        val startEpoch = System.currentTimeMillis()
        try {
            // 成员直接加入的组
            val groupIds = getGroupIdsByGroupMemberCondition(
                projectCode = projectCode,
                commonCondition = conditionReq
            )[MemberType.get(conditionReq.targetMember.type)]
            if (groupIds.isNullOrEmpty()) {
                return true
            }

            val targetMember = conditionReq.targetMember
            val memberGroupsDetailsList = listMemberGroupsDetails(
                projectCode = projectCode,
                memberId = targetMember.id,
                memberType = targetMember.type,
                groupIds = groupIds
            )
            val outOfSyncGroupIds = mutableListOf<Int>()
            val futures = groupIds.map { groupId ->
                CompletableFuture.supplyAsync(
                    {
                        val memberGroupsDetails = memberGroupsDetailsList.firstOrNull { it.id == groupId }
                        if (memberGroupsDetails == null) {
                            logger.warn("The data is out of sync, and the record no longer exists in the iam.$groupId")
                            outOfSyncGroupIds.add(groupId)
                            return@supplyAsync
                        }
                        val expiredAt = memberGroupsDetails.expiredAt
                        RetryUtils.retry(3) {
                            operateGroupMemberTask.invoke(
                                projectCode,
                                groupId,
                                conditionReq,
                                expiredAt
                            )
                        }
                    }, executorService
                )
            }
            handleFutures(
                projectCode = projectCode,
                outOfSyncGroupIds = outOfSyncGroupIds,
                futures = futures
            )
        } finally {
            "It take(${System.currentTimeMillis() - startEpoch})ms to $type group members|$projectCode|$conditionReq"
        }
        return true
    }

    private fun listMemberGroupsDetails(
        projectCode: String,
        memberId: String,
        memberType: String,
        groupIds: List<Int>
    ): List<MemberGroupDetailsResponse> {
        val memberGroupsDetailsList = mutableListOf<MemberGroupDetailsResponse>()
        val groupIdsChunk = groupIds.chunked(100)
        val futures = groupIdsChunk.map {
            CompletableFuture.supplyAsync(
                {
                    memberGroupsDetailsList.addAll(
                        // 若离职，则从数据库获取用户加入组的过期时间，调用iam接口会报错。
                        // 虽然数据库的过期时间可能不是最新的。
                        if (memberType == MemberType.USER.type && deptService.isUserDeparted(memberId)) {
                            val records = authResourceGroupMemberDao.listMemberGroupDetail(
                                dslContext = dslContext,
                                projectCode = projectCode,
                                memberId = memberId,
                                iamTemplateIds = emptyList(),
                                iamGroupIds = it
                            )
                            records.map { record ->
                                MemberGroupDetailsResponse().apply {
                                    id = record.iamGroupId
                                    expiredAt = record.expiredTime.timestamp()
                                }
                            }
                        } else {
                            iamV2ManagerService.listMemberGroupsDetails(
                                memberType,
                                memberId,
                                it.joinToString(",")
                            )
                        }
                    )
                }, executorService
            )
        }
        try {
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        } catch (ignore: Exception) {
            logger.warn("list member groups details failed!$ignore")
            throw ignore
        }
        return memberGroupsDetailsList
    }

    private fun handleFutures(
        projectCode: String,
        outOfSyncGroupIds: List<Int>,
        futures: List<CompletableFuture<Unit>>
    ) {
        try {
            CompletableFuture.allOf(*futures.toTypedArray()).join()
            // 存在iam那边已经把用户组下成员删除，但蓝盾数据库未同步问题
            outOfSyncGroupIds.forEach {
                syncIamGroupMemberService.syncIamGroupMember(
                    projectCode = projectCode,
                    iamGroupId = it
                )
            }
        } catch (ignore: Exception) {
            logger.warn("batch operate group members failed", ignore)
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_BATCH_OPERATE_GROUP_MEMBERS
            )
        }
    }

    private val url = "${config.devopsHostGateway}/console/permission/my-handover?type=handoverFromMe&flowNo=%s"

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        private val executorService = Executors.newFixedThreadPool(30)

        // 永久过期时间
        private const val PERMANENT_EXPIRED_TIME = 4102444800000L

        private const val HANDOVER_APPLICATION_RESULT_TEMPLATE_CODE = "BK_PERMISSIONS_HANDOVER_APPLICATION_RESULT"
    }
}
