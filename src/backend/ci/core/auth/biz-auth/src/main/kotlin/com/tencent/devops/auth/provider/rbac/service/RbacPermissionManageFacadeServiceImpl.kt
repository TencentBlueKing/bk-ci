package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.response.MemberGroupDetailsResponse
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ProjectMembersQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.enum.RemoveMemberButtonControl
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RbacPermissionManageFacadeServiceImpl(
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val groupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService,
    private val iamV2ManagerService: V2ManagerService,
    private val rbacCacheService: RbacCacheService,
    private val authAuthorizationDao: AuthAuthorizationDao,
    private val syncIamGroupMemberService: PermissionResourceGroupSyncService,
    private val permissionAuthorizationService: PermissionAuthorizationService
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
                action = action
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
            resourceGroupMembers = resourceGroupMembers
        )
        val records = mutableListOf<GroupDetailsInfoVo>()
        resourceGroupMembers.forEach {
            val resourceGroup = resourceGroupMap[it.iamGroupId.toString()]!!
            val groupMemberDetail = groupMemberDetailMap["${it.iamGroupId}_${it.memberId}"]
            records.add(
                convertGroupDetailsInfoVo(
                    resourceGroup = resourceGroup,
                    groupMemberDetail = groupMemberDetail,
                    uniqueManagerGroups = uniqueManagerGroups,
                    authResourceGroupMember = it
                )
            )
        }
        return SQLPage(count = count, records = records)
    }

    private fun getGroupMemberDetailMap(
        memberId: String,
        resourceGroupMembers: List<AuthResourceGroupMember>
    ): Map<String, MemberGroupDetailsResponse> {
        // 如果用户离职，查询权限中心接口会报错
        if (deptService.isUserDeparted(memberId)) {
            return emptyMap()
        }
        // 用户组成员详情
        val groupMemberDetailMap = mutableMapOf<String, MemberGroupDetailsResponse>()
        // 直接加入的用户
        val userGroupIds = resourceGroupMembers
            .filter { it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER) }
            .map { it.iamGroupId }
        if (userGroupIds.isNotEmpty()) {
            iamV2ManagerService.listMemberGroupsDetails(
                ManagerScopesEnum.getType(ManagerScopesEnum.USER),
                memberId,
                userGroupIds.joinToString(",")
            ).forEach {
                groupMemberDetailMap["${it.id}_$memberId"] = it
            }
        }
        // 直接加入的组织
        val deptGroupIds = resourceGroupMembers
            .filter { it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT) }
            .map { it.iamGroupId }
        if (deptGroupIds.isNotEmpty()) {
            iamV2ManagerService.listMemberGroupsDetails(
                ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT),
                memberId,
                deptGroupIds.joinToString(",")
            ).forEach {
                groupMemberDetailMap["${it.id}_$memberId"] = it
            }
        }
        // 人员模板加入的组
        resourceGroupMembers.filter { it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE) }
            .groupBy({ it.memberId }, { it.iamGroupId.toString() })
            .forEach { (iamTemplateId, iamGroupIds) ->
                if (iamGroupIds.isEmpty()) return@forEach
                iamV2ManagerService.listMemberGroupsDetails(
                    ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE),
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
        authResourceGroupMember: AuthResourceGroupMember
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
        return GroupDetailsInfoVo(
            resourceCode = resourceGroup.resourceCode,
            resourceName = resourceGroup.resourceName,
            resourceType = resourceGroup.resourceType,
            groupId = resourceGroup.relationId.toInt(),
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
                authResourceGroupMember.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE) ->
                    RemoveMemberButtonControl.TEMPLATE

                resourceGroup.resourceType == AuthResourceType.PROJECT.value &&
                    uniqueManagerGroups.contains(authResourceGroupMember.iamGroupId) ->
                    RemoveMemberButtonControl.UNIQUE_MANAGER

                uniqueManagerGroups.contains(authResourceGroupMember.iamGroupId) ->
                    RemoveMemberButtonControl.UNIQUE_OWNER

                else ->
                    RemoveMemberButtonControl.OTHER
            },
            joinedType = when (authResourceGroupMember.memberType) {
                ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE) -> JoinedType.TEMPLATE
                ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT) -> JoinedType.DEPARTMENT
                else -> JoinedType.DIRECT
            },
            operator = ""
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
        operateChannel: OperateChannel?
    ): List<MemberGroupCountWithPermissionsVo> {
        // 查询项目下包含该成员的组列表
        val projectGroupIds = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            memberId = memberId
        ).map { it.iamGroupId.toString() }
        // 通过项目组ID获取人员模板ID
        val iamTemplateId = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = projectGroupIds
        ).filter { it.iamTemplateId != null }
            .map { it.iamTemplateId.toString() }
        // 获取用户部门信息
        val memberDeptInfos = if (operateChannel == OperateChannel.PERSONAL) {
            deptService.getUserInfo(
                userId = "admin",
                name = memberId
            )?.deptInfo ?: return emptyList()
            deptService.getUserDeptInfo(memberId).toList()
        } else {
            emptyList()
        }

        val iamGroupIdsByConditions = listIamGroupIdsByConditions(
            condition = IamGroupIdsQueryConditionDTO(
                projectCode = projectCode,
                groupName = groupName,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action
            )
        )
        // 获取成员加入的用户组
        val memberGroupCountMap = authResourceGroupMemberDao.countMemberGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateId,
            iamGroupIds = iamGroupIdsByConditions,
            minExpiredAt = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) },
            maxExpiredAt = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) },
            memberDeptInfos = memberDeptInfos
        )
        val memberGroupCountList = mutableListOf<MemberGroupCountWithPermissionsVo>()
        // 项目排在第一位
        memberGroupCountMap[AuthResourceType.PROJECT.value]?.let { projectCount ->
            memberGroupCountList.add(
                MemberGroupCountWithPermissionsVo(
                    resourceType = AuthResourceType.PROJECT.value,
                    resourceTypeName = I18nUtil.getCodeLanMessage(
                        messageCode = AuthResourceType.PROJECT.value + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
                    ),
                    count = projectCount
                )
            )
        }

        rbacCacheService.listResourceTypes()
            .filter { it.resourceType != AuthResourceType.PROJECT.value }
            .forEach { resourceTypeInfoVo ->
                memberGroupCountMap[resourceTypeInfoVo.resourceType]?.let { count ->
                    val memberGroupCount = MemberGroupCountWithPermissionsVo(
                        resourceType = resourceTypeInfoVo.resourceType,
                        resourceTypeName = I18nUtil.getCodeLanMessage(
                            messageCode = resourceTypeInfoVo.resourceType + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX,
                            defaultMessage = resourceTypeInfoVo.name
                        ),
                        count = count
                    )
                    memberGroupCountList.add(memberGroupCount)
                }
            }

        return memberGroupCountList
    }

    override fun listIamGroupIdsByConditions(condition: IamGroupIdsQueryConditionDTO): List<Int> {
        return with(condition) {
            val filterGroupsByGroupName = if (isQueryByGroupName()) {
                permissionResourceGroupService.listIamGroupIdsByGroupName(
                    projectId = projectCode,
                    groupName = groupName!!
                )
            } else {
                emptyList()
            }
            val finalGroupIds = if (isQueryByGroupPermissions()) {
                groupPermissionService.listGroupsByPermissionConditions(
                    projectCode = projectCode,
                    filterIamGroupIds = filterGroupsByGroupName,
                    relatedResourceType = relatedResourceType!!,
                    relatedResourceCode = relatedResourceCode,
                    action = action
                )
            } else {
                filterGroupsByGroupName
            }.toMutableList()
            iamGroupIds?.let { finalGroupIds.addAll(it) }
            finalGroupIds
        }
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
        excludeIamGroupIds: List<Int>?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        operateChannel: OperateChannel?,
        start: Int?,
        limit: Int?
    ): Pair<Long, List<AuthResourceGroupMember>> {
        // 获取用户加入的项目级用户组模板ID
        val iamTemplateIds = listProjectMemberGroupTemplateIds(
            projectCode = projectCode,
            memberId = memberId
        )
        // 获取用户所属组织
        val memberDeptInfos = if (operateChannel == OperateChannel.PERSONAL) {
            getMemberDeptInfos(memberId)
        } else {
            emptyList()
        }

        val minExpiredTime = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val maxExpiredTime = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val count = authResourceGroupMemberDao.countMemberGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            excludeIamGroupIds = excludeIamGroupIds,
            minExpiredAt = minExpiredTime,
            maxExpiredAt = maxExpiredTime,
            memberDeptInfos = memberDeptInfos
        )[resourceType] ?: 0L
        val resourceGroupMembers = authResourceGroupMemberDao.listMemberGroupDetail(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            excludeIamGroupIds = excludeIamGroupIds,
            minExpiredAt = minExpiredTime,
            maxExpiredAt = maxExpiredTime,
            memberDeptInfos = memberDeptInfos,
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
        commonCondition: GroupMemberCommonConditionReq
    ): Map<ManagerScopesEnum, List<Int>> {
        val finalResourceGroupMembers = mutableListOf<AuthResourceGroupMember>()
        with(commonCondition) {
            // 1.根据条件筛选出用户组
            val resourceGroupMembersByCondition = when {
                // 全选
                allSelection -> {
                    listResourceGroupMembers(
                        projectCode = projectCode,
                        memberId = commonCondition.targetMember.id,
                        operateChannel = commonCondition.operateChannel
                    ).second
                }
                // 全选某些资源类型用户组
                resourceTypes.isNotEmpty() -> {
                    resourceTypes.flatMap { resourceType ->
                        listResourceGroupMembers(
                            projectCode = projectCode,
                            memberId = commonCondition.targetMember.id,
                            resourceType = resourceType,
                            operateChannel = commonCondition.operateChannel
                        ).second
                    }
                }

                else -> {
                    emptyList()
                }
            }

            if (resourceGroupMembersByCondition.isNotEmpty()) {
                finalResourceGroupMembers.addAll(resourceGroupMembersByCondition)
            }

            if (groupIds.isNotEmpty()) {
                val resourceGroupMembersOfSelect = listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = commonCondition.targetMember.id,
                    iamGroupIds = groupIds
                ).second
                finalResourceGroupMembers.addAll(resourceGroupMembersOfSelect)
            }

            // 2.进行分类，直接/模板/部门加入
            val groupIdsOfDirectJoined = finalResourceGroupMembers.filter {
                it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE)
            }.map { it.iamGroupId }.toMutableList()

            val groupInfoIdsOfTemplateJoined = finalResourceGroupMembers.filter {
                it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER)
            }.map { it.iamGroupId }.toMutableList()

            val groupInfoIdsOfDepartmentJoined = finalResourceGroupMembers.filter {
                it.memberType == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)
            }.map { it.iamGroupId }.toMutableList()

            // 3.根据条件排除用户组。
            // 3.1 排除唯一管理组（将用户移出用户组时，需进行排除）
            if (excludedUniqueManagerGroup) {
                val excludedUniqueManagerGroupIds = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    iamGroupIds = groupIdsOfDirectJoined
                )
                groupIdsOfDirectJoined.removeAll {
                    excludedUniqueManagerGroupIds.contains(it)
                }
            }
            // 4.组装返回结果。
            val result = mutableMapOf<ManagerScopesEnum, List<Int>>()
            if (groupIdsOfDirectJoined.isNotEmpty()) {
                result[ManagerScopesEnum.USER] = groupIdsOfDirectJoined
            }
            if (groupInfoIdsOfTemplateJoined.isNotEmpty()) {
                result[ManagerScopesEnum.TEMPLATE] = groupInfoIdsOfTemplateJoined
            }
            if (groupInfoIdsOfDepartmentJoined.isNotEmpty()) {
                result[ManagerScopesEnum.DEPARTMENT] = groupInfoIdsOfTemplateJoined
            }
            return result
        }
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
        iamGroupIds: List<Int>,
        memberId: String
    ): Pair<List<Int>, List<String>> {
        logger.info("list invalid authorizations after operated groups:$projectCode|$iamGroupIds|$memberId")
        deptService.getUserInfo(memberId, memberId) ?: return Pair(emptyList(), emptyList())
        // 1.筛选出本次退出/交接中包含流水线执行权限的用户组
        val operatedGroupsWithExecutePerm = groupPermissionService.listGroupsByPermissionConditions(
            projectCode = projectCode,
            relatedResourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            action = ActionId.PIPELINE_EXECUTE,
            filterIamGroupIds = iamGroupIds
        )
        logger.debug("list operated groups with execute perm:{}", operatedGroupsWithExecutePerm)

        // 2.获取用户退出/交接以上操作的用户组后，还未退出的流水线/项目级别（仅这些类型会包含流水线执行权限）的用户组。
        val userGroupsJoinedAfterOperatedGroups = listResourceGroupMembers(
            projectCode = projectCode,
            memberId = memberId,
            resourceType = ResourceTypeId.PIPELINE,
            excludeIamGroupIds = operatedGroupsWithExecutePerm
        ).second.toMutableList().apply {
            addAll(
                listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = memberId,
                    resourceType = ResourceTypeId.PROJECT,
                    excludeIamGroupIds = operatedGroupsWithExecutePerm
                ).second
            )
        }.map { it.iamGroupId }
        logger.debug("list user groups joined after operated groups:{}", userGroupsJoinedAfterOperatedGroups)
        // 3.查询未退出的流水线/项目级别的用户组中是否包含项目级别的流水线执行权限。
        // 查询用户在未退出的用户组中否还有整个项目的流水线执行权限。若有的话，则对流水线的代持人权限未造成影响。
        val hasAllPipelineExecutePermAfterOperateGroups = groupPermissionService.isGroupsHasProjectLevelPermission(
            projectCode = projectCode,
            filterIamGroupIds = userGroupsJoinedAfterOperatedGroups,
            action = ActionId.PIPELINE_EXECUTE
        )
        logger.debug("has all pipeline execute perm after operate groups:{}", hasAllPipelineExecutePermAfterOperateGroups)

        // 3.1.若用户在未退出的组中拥有整个项目的流水线执行权限，则本次不会对任何的流水线代持人权限造成影响。
        if (hasAllPipelineExecutePermAfterOperateGroups)
            return Pair(emptyList(), emptyList())

        // 3.2.若没有的话，查询本次退出/交接的用户组中是否包含项目级别的流水线执行权限。
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
            logger.debug("pipelines with execute perm after operate groups:{}", pipelinesWithExecutePermAfterOperatedGroups)

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
            return Pair(operatedGroupsWithExecutePerm, pipelinesWithoutAuthorization)
        }
        return Pair(emptyList(), emptyList())
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
            conditionReq = GroupMemberRenewalConditionReq(
                groupIds = listOf(groupId),
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
        if (targetMember.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
            deptService.isUserDeparted(targetMember.id)) {
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
        return expiredAt < PERMANENT_EXPIRED_TIME
    }

    override fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean {
        logger.info("batch renewal group member $userId|$projectCode|$renewalConditionReq")
        batchOperateGroupMembers(
            projectCode = projectCode,
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
        logger.info("batch handover group members $userId|$projectCode|$handoverMemberDTO")
        handoverMemberDTO.checkHandoverTo()
        batchOperateGroupMembers(
            projectCode = projectCode,
            conditionReq = handoverMemberDTO,
            operateGroupMemberTask = ::handoverTask
        )
        return true
    }

    override fun batchDeleteResourceGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberCommonConditionReq
    ): Boolean {
        logger.info("batch delete group members $userId|$projectCode|$removeMemberDTO")
        removeMemberDTO.excludedUniqueManagerGroup = true
        batchOperateGroupMembers(
            projectCode = projectCode,
            conditionReq = removeMemberDTO,
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
        when {
            // 若权限已过期，如果是唯一管理员组，允许交接，交接人将获得半年权限；其他的直接删除。
            expiredAt < currentTimeSeconds -> {
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
                        removeMemberDTO = GroupMemberCommonConditionReq(
                            targetMember = handoverMemberDTO.targetMember
                        ),
                        expiredAt = finalExpiredAt
                    )
                    return
                }
            }
            // 若交接人已经在用户组内，无需交接。
            authResourceGroupMemberDao.isMemberInGroup(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupId = groupId,
                memberId = handoverMemberDTO.handoverTo.id
            ) -> {
                deleteTask(
                    projectCode = projectCode,
                    groupId = groupId,
                    removeMemberDTO = GroupMemberCommonConditionReq(
                        targetMember = handoverMemberDTO.targetMember
                    ),
                    expiredAt = finalExpiredAt
                )
                return
            }
        }

        val members = listOf(
            ManagerMember(
                handoverMemberDTO.handoverTo.type,
                handoverMemberDTO.handoverTo.id
            )
        )
        if (finalExpiredAt < currentTimeSeconds) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_EXPIRED_PERM_NOT_ALLOW_TO_HANDOVER
            )
        }

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
        removeMemberDTO: GroupMemberCommonConditionReq,
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
        // 获取用户加入的用户组
        val joinedType2GroupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = conditionReq
        )
        val groupIdsOfDirectJoined = joinedType2GroupIds[ManagerScopesEnum.USER] ?: emptyList()
        val groupInfoIdsOfTemplateJoined = joinedType2GroupIds[ManagerScopesEnum.TEMPLATE] ?: emptyList()

        val totalCount = groupIdsOfDirectJoined.size + groupInfoIdsOfTemplateJoined.size
        val groupCountOfTemplateJoined = groupInfoIdsOfTemplateJoined.size

        return when (batchOperateType) {
            BatchOperateType.REMOVE -> {
                val groupCountOfUniqueManager = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    iamGroupIds = groupIdsOfDirectJoined
                ).size
                BatchOperateGroupMemberCheckVo(
                    totalCount = totalCount,
                    inoperableCount = groupCountOfUniqueManager + groupCountOfTemplateJoined
                )
            }

            BatchOperateType.RENEWAL -> {
                with(conditionReq) {
                    val isUserDeparted = targetMember.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
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
                            groupIds = groupIdsOfDirectJoined
                        ).filter {
                            // iam用的是秒级时间戳
                            it.expiredAt == PERMANENT_EXPIRED_TIME / 1000
                        }.size
                        BatchOperateGroupMemberCheckVo(
                            totalCount = totalCount,
                            inoperableCount = groupCountOfPermanentExpiredTime + groupCountOfTemplateJoined
                        )
                    }
                }
            }

            BatchOperateType.HANDOVER -> {
                // 已过期（除唯一管理员组）或通过模板加入的不允许移交
                with(conditionReq) {
                    val finalGroupIds = groupIdsOfDirectJoined.toMutableList()
                    val uniqueManagerGroupIds = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        iamGroupIds = groupIdsOfDirectJoined
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
                    BatchOperateGroupMemberCheckVo(
                        totalCount = totalCount,
                        inoperableCount = groupCountOfTemplateJoined + groupCountOfExpired
                    )
                }
            }

            else -> {
                BatchOperateGroupMemberCheckVo(
                    totalCount = totalCount,
                    inoperableCount = groupCountOfTemplateJoined
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
            val isNeedToHandover = handoverTo != null
            if (memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER) && isNeedToHandover) {
                removeMemberFromProjectReq.checkHandoverTo()
                val handoverMemberDTO = GroupMemberHandoverConditionReq(
                    allSelection = true,
                    targetMember = targetMember,
                    handoverTo = handoverTo!!
                )
                batchOperateGroupMembers(
                    projectCode = projectCode,
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
                val removeMemberDTO = GroupMemberCommonConditionReq(
                    allSelection = true,
                    targetMember = targetMember
                )
                batchOperateGroupMembers(
                    projectCode = projectCode,
                    conditionReq = removeMemberDTO,
                    operateGroupMemberTask = ::deleteTask
                )
            }

            if (memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
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
                        memberType = ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)
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
            if (targetMember.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
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

    private fun <T : GroupMemberCommonConditionReq> batchOperateGroupMembers(
        projectCode: String,
        conditionReq: T,
        operateGroupMemberTask: (
            projectCode: String,
            groupId: Int,
            conditionReq: T,
            expiredAt: Long
        ) -> Unit
    ): Boolean {
        // 直接加入的组
        val groupIds = getGroupIdsByGroupMemberCondition(
            projectCode = projectCode,
            commonCondition = conditionReq
        )[ManagerScopesEnum.USER]
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
                        logger.warn(
                            "The data is out of sync, and the record no longer exists in the iam.$groupId"
                        )
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
                        if (memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
                            deptService.isUserDeparted(userId = memberId)) {
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

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        private val executorService = Executors.newFixedThreadPool(30)

        // 永久过期时间
        private const val PERMANENT_EXPIRED_TIME = 4102444800000L
    }
}
