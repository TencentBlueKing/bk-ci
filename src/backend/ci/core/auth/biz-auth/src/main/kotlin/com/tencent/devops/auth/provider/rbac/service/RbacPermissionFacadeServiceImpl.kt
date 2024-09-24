package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.response.MemberGroupDetailsResponse
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.enum.RemoveMemberButtonControl
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RbacPermissionFacadeServiceImpl(
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val groupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService,
    private val iamV2ManagerService: V2ManagerService,
    private val rbacCacheService: RbacCacheService
) : PermissionFacadeService {
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
        // 查询成员所在资源用户组列表，直接加入+通过用户组（模板）加入
        val (count, resourceGroupMembers) = permissionResourceMemberService.listResourceGroupMembers(
            projectCode = projectId,
            memberId = memberId,
            resourceType = resourceType,
            iamGroupIds = iamGroupIdsByConditions,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
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
        action: String?
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

        val iamGroupIdsByConditions = listIamGroupIdsByConditions(
            condition = IamGroupIdsQueryConditionDTO(
                projectCode = projectCode,
                groupName = groupName,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action
            )
        )
        // 获取成员直接加入的组和通过模板加入的组
        val memberGroupCountMap = authResourceGroupMemberDao.countMemberGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateId,
            iamGroupIds = iamGroupIdsByConditions,
            minExpiredAt = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) },
            maxExpiredAt = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
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

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        // 永久过期时间
        private const val PERMANENT_EXPIRED_TIME = 4102444800000L
    }
}
