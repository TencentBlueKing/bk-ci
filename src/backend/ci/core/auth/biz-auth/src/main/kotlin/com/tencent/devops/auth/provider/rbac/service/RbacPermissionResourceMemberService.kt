package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.GroupMemberRenewApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.vo.ResourceMemberCountVO
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.constant.ProjectMessageCode
import org.apache.commons.lang3.RandomUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("SpreadOperator", "LongParameterList")
class RbacPermissionResourceMemberService(
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService
) : PermissionResourceMemberService {
    override fun getResourceGroupMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthGroup?
    ): List<String> {
        logger.info("[RBAC-IAM] get resource group members:$projectCode|$resourceType|$resourceCode|$group")
        return when (group) {
            // 新的rbac版本中，没有ci管理员组，不可以调用此接口来获取ci管理员组的成员
            BkAuthGroup.CIADMIN, BkAuthGroup.CI_MANAGER -> emptyList()
            // 获取特定资源下全部成员
            null -> {
                authResourceGroupMemberDao.listResourceGroupMember(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    minExpiredTime = LocalDateTime.now(),
                    memberType = MemberType.USER.type
                ).map { it.memberId }.distinct()
            }
            // 获取特定资源下特定用户组成员
            else -> {
                authResourceGroupMemberDao.listResourceGroupMember(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    minExpiredTime = LocalDateTime.now(),
                    groupCode = group.value,
                    memberType = MemberType.USER.type
                ).map { it.memberId }
            }
        }
    }

    override fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList> {
        val groupId2Members = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            minExpiredTime = LocalDateTime.now()
        ).groupBy { it.iamGroupId }
        val resourceGroupInfos = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = groupId2Members.keys.map { it.toString() }
        )
        val result = mutableListOf<BkAuthGroupAndUserList>()

        groupId2Members.forEach { (groupId, members) ->
            val userMembers = members.filter { it.memberType == MemberType.USER.type }
            val deptMembers = members.filter { it.memberType == MemberType.DEPARTMENT.type }
            val groupName = resourceGroupInfos.firstOrNull { it.relationId.toInt() == groupId }?.groupName
                ?: return@forEach
            result.add(
                BkAuthGroupAndUserList(
                    displayName = groupName,
                    roleId = groupId,
                    roleName = groupName,
                    userIdList = userMembers.map { it.memberId },
                    deptInfoList = deptMembers.map { deptInfo ->
                        RoleGroupMemberInfo().apply {
                            id = deptInfo.memberId
                            name = deptInfo.memberName
                        }
                    },
                    type = ""
                )
            )
        }
        return result
    }

    override fun getProjectMemberCount(projectCode: String): ResourceMemberCountVO {
        val projectMemberCount = authResourceGroupMemberDao.countProjectMember(
            dslContext = dslContext,
            projectCode = projectCode
        )
        return ResourceMemberCountVO(
            userCount = projectMemberCount[MemberType.USER.type] ?: 0,
            departmentCount = projectMemberCount[MemberType.DEPARTMENT.type] ?: 0
        )
    }

    override fun listProjectMembers(
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): SQLPage<ResourceMemberInfo> {
        logger.info("list project members:$projectCode|$departedFlag|$memberType|$userName|$deptName")
        if (!userName.isNullOrEmpty() && !deptName.isNullOrEmpty()) {
            return SQLPage(count = 0, records = emptyList())
        }

        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val count = authResourceGroupMemberDao.countProjectMember(
            dslContext = dslContext,
            projectCode = projectCode,
            memberType = memberType,
            userName = userName,
            deptName = deptName
        )
        val records = authResourceGroupMemberDao.listProjectMembers(
            dslContext = dslContext,
            projectCode = projectCode,
            memberType = memberType,
            userName = userName,
            deptName = deptName,
            offset = limit.offset,
            limit = limit.limit
        )

        // 不查询离职相关信息，防止调用用户管理接口，响应慢
        if (departedFlag == false) {
            return SQLPage(count = count, records = records)
        }

        return SQLPage(count = count, records = addDepartedFlagToMembers(records))
    }

    override fun addDepartedFlagToMembers(records: List<ResourceMemberInfo>): List<ResourceMemberInfo> {
        val userMembers = records.filter {
            it.type == MemberType.USER.type
        }.map { it.id }
        val departedMembers = if (userMembers.isNotEmpty()) {
            deptService.listDepartedMembers(
                memberIds = userMembers
            )
        } else {
            return records
        }
        return records.map {
            if (it.type != MemberType.USER.type) {
                it.copy(departed = false)
            } else {
                it.copy(departed = departedMembers.contains(it.id))
            }
        }
    }

    override fun addGroupMember(
        projectCode: String,
        memberId: String,
        /*user 或 department*/
        memberType: String,
        expiredAt: Long,
        iamGroupId: Int
    ): Boolean {
        if (memberType == MemberType.USER.type &&
            deptService.isUserDeparted(memberId)) {
            return true
        }
        // 获取对应的资源组
        val authResourceGroup = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            relationId = iamGroupId.toString()
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.GROUP_NOT_EXIST
        )
        val managerMember = ManagerMember(memberType, memberId)
        addIamGroupMember(
            groupId = iamGroupId,
            members = listOf(managerMember),
            expiredAt = expiredAt
        )

        val memberDetails = deptService.getMemberInfo(
            memberId = memberId,
            memberType = ManagerScopesEnum.valueOf(memberType.uppercase())
        )
        with(authResourceGroup) {
            authResourceGroupMemberDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = groupCode,
                iamGroupId = relationId.toInt(),
                memberId = memberId,
                memberName = memberDetails.displayName,
                memberType = memberType,
                expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(expiredAt)
            )
        }
        return true
    }

    override fun addIamGroupMember(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean {
        val membersOfNeedToAdd = members.toMutableList().removeDepartedMembers()
        if (membersOfNeedToAdd.isNotEmpty()) {
            val managerMemberGroup =
                ManagerMemberGroupDTO.builder().members(membersOfNeedToAdd).expiredAt(expiredAt).build()
            iamV2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroup)
        }
        return true
    }

    override fun batchAddResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>?,
        departments: List<String>?
    ): Boolean {
        // 校验用户组是否属于该项目
        verifyGroupBelongToProject(
            projectCode = projectCode,
            iamGroupId = iamGroupId
        )
        // 获取用户组中用户以及部门
        val userType = MemberType.USER.type
        val deptType = MemberType.DEPARTMENT.type
        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        val groupMembers = iamV2ManagerService.getRoleGroupMemberV2(iamGroupId, pageInfoDTO).results
        val groupUserMap = groupMembers.filter { it.type == userType }.associateBy { it.id }
        val groupDepartmentSet = groupMembers.filter { it.type == deptType }.map { it.id }.toSet()
        // 校验用户是否应该加入用户组
        val iamMemberInfos = mutableListOf<ManagerMember>()
        if (!members.isNullOrEmpty()) {
            val departedMembers = deptService.listDepartedMembers(
                memberIds = members
            )
            members.filterNot { departedMembers.contains(it) }.forEach {
                val shouldAddUserToGroup = shouldAddUserToGroup(
                    groupUserMap = groupUserMap,
                    groupDepartmentSet = groupDepartmentSet,
                    member = it
                )
                if (shouldAddUserToGroup) {
                    iamMemberInfos.add(ManagerMember(userType, it))
                }
            }
        }
        if (!departments.isNullOrEmpty()) {
            departments.forEach {
                if (!groupDepartmentSet.contains(it)) {
                    iamMemberInfos.add(ManagerMember(deptType, it))
                }
            }
        }

        logger.info("batch add project user:|$projectCode|$iamGroupId|$expiredTime|$iamMemberInfos")
        if (iamMemberInfos.isNotEmpty()) {
            addIamGroupMember(
                groupId = iamGroupId,
                members = iamMemberInfos,
                expiredAt = expiredTime
            )
            // 获取对应的资源组
            val authResourceGroup = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                relationId = iamGroupId.toString()
            ) ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.GROUP_NOT_EXIST
            )
            val groupMembersList = mutableListOf<AuthResourceGroupMember>()
            iamMemberInfos.forEach {
                val memberDetails = deptService.getMemberInfo(
                    memberId = it.id,
                    memberType = ManagerScopesEnum.valueOf(it.type.uppercase())
                )
                groupMembersList.add(
                    AuthResourceGroupMember(
                        projectCode = projectCode,
                        resourceType = authResourceGroup.resourceType,
                        resourceCode = authResourceGroup.resourceCode,
                        groupCode = authResourceGroup.groupCode,
                        iamGroupId = authResourceGroup.relationId.toInt(),
                        memberId = it.id,
                        memberName = memberDetails.displayName,
                        memberType = it.type,
                        expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(expiredTime)
                    )
                )
            }
            authResourceGroupMemberDao.batchCreate(
                dslContext = dslContext,
                groupMembers = groupMembersList
            )
        }
        return true
    }

    private fun verifyGroupBelongToProject(
        projectCode: String,
        iamGroupId: Int
    ) {
        val isGroupBelongToProject = authResourceGroupDao.isGroupBelongToProject(
            dslContext = dslContext,
            projectCode = projectCode,
            groupId = iamGroupId.toString()
        )
        if (!isGroupBelongToProject) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.ERROR_GROUP_NOT_BELONG_TO_PROJECT,
                defaultMessage = "The group($iamGroupId) does not belong to the project($projectCode)!"
            )
        }
    }

    private fun shouldAddUserToGroup(
        groupUserMap: Map<String, RoleGroupMemberInfo>,
        groupDepartmentSet: Set<String>,
        member: String
    ): Boolean {
        // 校验是否将用户加入组，如果用户已经在用户组,并且过期时间超过30天,则不再添加
        val expectExpiredAt = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(VALID_EXPIRED_AT)
        if (groupUserMap.containsKey(member) && groupUserMap[member]!!.expiredAt > expectExpiredAt) {
            return false
        }
        // 校验用户的部门是否已经加入组，若部门已经加入，则不再添加该用户
        try {
            val userDeptInfoSet = deptService.getUserDeptInfo(userId = member)
            val isUserBelongGroupByDepartments = groupDepartmentSet.intersect(userDeptInfoSet).isNotEmpty()
            if (isUserBelongGroupByDepartments) {
                return false
            }
        } catch (ignore: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = arrayOf(member),
                defaultMessage = "user $member not exist"
            )
        }
        return true
    }

    override fun batchDeleteResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        members: List<String>?,
        departments: List<String>?
    ): Boolean {
        logger.info("batch delete resource group members :|$projectCode|$iamGroupId||$members|$departments")
        verifyGroupBelongToProject(
            projectCode = projectCode,
            iamGroupId = iamGroupId
        )
        val userType = MemberType.USER.type
        val deptType = MemberType.DEPARTMENT.type
        val allMemberIds = mutableListOf<String>()
        if (!members.isNullOrEmpty()) {
            deleteIamGroupMembers(
                groupId = iamGroupId,
                type = userType,
                memberIds = members
            )
            allMemberIds.addAll(members)
        }
        if (!departments.isNullOrEmpty()) {
            deleteIamGroupMembers(
                groupId = iamGroupId,
                type = deptType,
                memberIds = departments
            )
            allMemberIds.addAll(departments)
        }
        authResourceGroupMemberDao.batchDeleteGroupMembers(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId,
            memberIds = allMemberIds
        )
        return true
    }

    private fun getGroupInfoList(
        resourceType: String,
        managerId: String
    ): List<V2ManagerRoleGroupInfo> {
        return if (resourceType == AuthResourceType.PROJECT.value) {
            val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
            val pageInfoDTO = V2PageInfoDTO()
            pageInfoDTO.page = 1
            pageInfoDTO.pageSize = 1000
            val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
                managerId,
                searchGroupDTO,
                pageInfoDTO
            )
            iamGroupInfoList.results
        } else {
            val v2PageInfoDTO = V2PageInfoDTO().apply {
                pageSize = 1000
                page = 1
            }
            iamV2ManagerService.getSubsetManagerRoleGroup(
                managerId.toInt(),
                v2PageInfoDTO
            ).results
        }
    }

    private fun getUsersUnderGroup(groupInfo: V2ManagerRoleGroupInfo): BkAuthGroupAndUserList {
        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(groupInfo.id, pageInfoDTO).results

        val nowTimestamp = System.currentTimeMillis() / 1000
        val (members, deptInfoList) = groupMemberInfoList
            .filter { it.expiredAt > nowTimestamp }
            .partition { it.type == MemberType.USER.type }

        return BkAuthGroupAndUserList(
            displayName = groupInfo.name,
            roleId = groupInfo.id,
            roleName = groupInfo.name,
            userIdList = members.map { it.id },
            deptInfoList = deptInfoList.map { memberInfo ->
                RoleGroupMemberInfo().apply {
                    id = memberInfo.id
                    name = memberInfo.name
                }
            },
            type = ""
        )
    }

    override fun roleCodeToIamGroupId(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        roleCode: String
    ): Int {
        return if (roleCode == BkAuthGroup.CI_MANAGER.value) {
            authResourceGroupDao.getByGroupName(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupName = BkAuthGroup.CI_MANAGER.groupName
            )?.relationId
        } else {
            authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = roleCode
            )?.relationId
        }?.toInt() ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST,
            params = arrayOf(roleCode),
            defaultMessage = "group $roleCode not exist"
        )
    }

    override fun autoRenewal(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        validExpiredDay: Int
    ) {
        // 1、获取分级管理员或者二级管理员ID
        val managerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).relationId
        // 2、获取分级管理员下所有的用户组
        val iamGroupInfoList = getGroupInfoList(
            resourceType = resourceType,
            managerId = managerId
        )
        // 3. 获取由蓝盾创建的用户组列表
        val resourceGroupInfoList = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = iamGroupInfoList.map { it.id.toString() }
        )
        val currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        // 预期的自动过期天数
        val expectAutoExpiredAt = currentTime + TimeUnit.DAYS.toSeconds(validExpiredDay.toLong())
        val autoRenewalMembers = mutableSetOf<String>()
        resourceGroupInfoList.forEach group@{ resourceGroup ->
            val iamGroupId = resourceGroup.relationId.toInt()
            val pageInfoDTO = V2PageInfoDTO().apply {
                pageSize = 1000
                page = 1
            }
            val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(iamGroupId, pageInfoDTO).results
            groupMemberInfoList.forEach member@{ member ->
                // 已过期或者小于自动续期范围内的不做续期
                if (member.expiredAt < currentTime ||
                    member.expiredAt > expectAutoExpiredAt
                ) {
                    val dataTime = DateTimeUtil.convertTimestampToLocalDateTime(member.expiredAt)
                    logger.info("Group member does not need to be renewed|$iamGroupId|$member|$dataTime")
                    return@member
                }
                // 自动续期时间由半年+随机天数,防止同一时间同时过期
                val expiredTime = currentTime + AUTO_RENEWAL_EXPIRED_AT +
                    TimeUnit.DAYS.toSeconds(RandomUtils.nextLong(0, 180))
                autoRenewalMembers.add(member.id)
                try {
                    addGroupMember(
                        projectCode = projectCode,
                        memberId = member.id,
                        memberType = member.type,
                        expiredAt = expiredTime,
                        iamGroupId = iamGroupId
                    )
                } catch (ignored: Exception) {
                    // 用户不存在时,iam会抛异常
                    logger.error(
                        "auto renewal member, user not existed||$projectCode|$resourceType|$resourceCode",
                        ignored
                    )
                }
            }
        }
        if (autoRenewalMembers.isNotEmpty()) {
            logger.info("auto renewal member|$projectCode|$resourceType|$resourceCode|$autoRenewalMembers")
        }
    }

    override fun renewalGroupMember(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Boolean {
        logger.info("renewal group member|$userId|$projectCode|$resourceType|$groupId|${memberRenewalDTO.expiredAt}")
        val managerMemberGroupDTO = GroupMemberRenewApplicationDTO.builder()
            .groupIds(listOf(groupId))
            .expiredAt(memberRenewalDTO.expiredAt)
            .reason("renewal user group")
            .applicant(userId).build()
        iamV2ManagerService.renewalRoleGroupMemberApplication(managerMemberGroupDTO)
        return true
    }

    override fun renewalIamGroupMembers(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean {
        val membersOfNeedToRenewal = members.toMutableList().removeDepartedMembers()
        if (membersOfNeedToRenewal.isNotEmpty()) {
            iamV2ManagerService.renewalRoleGroupMemberV2(
                groupId,
                ManagerMemberGroupDTO.builder()
                    .members(membersOfNeedToRenewal)
                    .expiredAt(expiredAt)
                    .build()
            )
        }
        return true
    }

    override fun deleteIamGroupMembers(
        groupId: Int,
        type: String,
        memberIds: List<String>
    ): Boolean {
        val membersOfNeedToDelete = if (type == MemberType.USER.type) {
            memberIds.filterNot { deptService.isUserDeparted(it) }
        } else {
            memberIds
        }
        if (membersOfNeedToDelete.isNotEmpty()) {
            iamV2ManagerService.deleteRoleGroupMemberV2(
                groupId,
                type,
                membersOfNeedToDelete.joinToString(",")
            )
        }
        return true
    }

    private fun MutableList<ManagerMember>.removeDepartedMembers(): List<ManagerMember> {
        val userMemberIds = this.filter { it.type == MemberType.USER.type }.map { it.id }
        if (userMemberIds.isEmpty()) return this
        // 获取离职的人员
        val departedMembers = deptService.listDepartedMembers(
            memberIds = userMemberIds
        )
        return this.filterNot {
            it.type == MemberType.USER.type &&
                departedMembers.contains(it.id)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        // 有效的过期时间,在30天内就是有效的
        private const val VALID_EXPIRED_AT = 30L

        // 自动续期有效的过期时间,在180天以上就不需要自动续期
        private val AUTO_VALID_EXPIRED_AT = TimeUnit.DAYS.toSeconds(180)

        // 自动续期默认180天
        private val AUTO_RENEWAL_EXPIRED_AT = TimeUnit.DAYS.toSeconds(180)

        private val executorService = Executors.newFixedThreadPool(30)
    }
}
