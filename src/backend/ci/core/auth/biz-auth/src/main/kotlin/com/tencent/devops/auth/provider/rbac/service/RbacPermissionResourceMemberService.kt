package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.GroupMemberRenewApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.dto.response.MemberGroupDetailsResponse
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ProjectMembersQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceMemberCountVO
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.iam.PermissionFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.project.constant.ProjectMessageCode
import org.apache.commons.lang3.RandomUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("SpreadOperator", "LongParameterList")
class RbacPermissionResourceMemberService constructor(
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService,
    private val permissionAuthorizationService: PermissionAuthorizationService,
    private val syncIamGroupMemberService: PermissionResourceGroupSyncService,
    private val permissionFacadeService: PermissionFacadeService
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
                getResourceGroupAndMembers(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ).flatMap { it.userIdList }.distinct()
            }
            // 获取特定资源下特定用户组成员
            else -> {
                val dbGroupInfo = authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = group.value
                ) ?: return emptyList()
                val groupInfo = getResourceGroupAndMembers(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ).find { it.roleId == dbGroupInfo.relationId.toInt() }
                groupInfo?.userIdList ?: emptyList()
            }
        }
    }

    override fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList> {
        // 1、获取管理员id
        val managerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).relationId
        // 2、获取分级管理员下所有的用户组
        val groupInfoList = getGroupInfoList(
            resourceType = resourceType,
            managerId = managerId
        )
        logger.info(
            "[RBAC-IAM] getResourceGroupAndMembers: projectCode = $projectCode |" +
                " managerId = $managerId | groupInfoList: $groupInfoList"
        )
        // 3、获取组成员
        return groupInfoList.map {
            executorService.submit<BkAuthGroupAndUserList> {
                getUsersUnderGroup(groupInfo = it)
            }
        }.map { it.get() }
    }

    override fun getProjectMemberCount(projectCode: String): ResourceMemberCountVO {
        val projectMemberCount = authResourceGroupMemberDao.countProjectMember(
            dslContext = dslContext,
            projectCode = projectCode
        )
        return ResourceMemberCountVO(
            userCount = projectMemberCount[ManagerScopesEnum.getType(ManagerScopesEnum.USER)] ?: 0,
            departmentCount = projectMemberCount[ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)] ?: 0
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
        val records = authResourceGroupMemberDao.listProjectMember(
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

    private fun addDepartedFlagToMembers(records: List<ResourceMemberInfo>): List<ResourceMemberInfo> {
        val userMembers = records.filter {
            it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)
        }.map { it.id }
        val departedMembers = if (userMembers.isNotEmpty()) {
            deptService.listDepartedMembers(
                memberIds = userMembers
            )
        } else {
            return records
        }
        return records.map {
            if (it.type != ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                it.copy(departed = false)
            } else {
                it.copy(departed = departedMembers.contains(it.id))
            }
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
            return listProjectMembers(
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
            permissionFacadeService.listIamGroupIdsByConditions(
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
            }
        }

        val records = authResourceGroupMemberDao.listProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )

        val count = authResourceGroupMemberDao.countProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )

        // 添加离职标志
        return if (conditionDTO.departedFlag == false) {
            SQLPage(count, records)
        } else {
            SQLPage(count, addDepartedFlagToMembers(records))
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
        if (memberType == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
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
        val userType = ManagerScopesEnum.getType(ManagerScopesEnum.USER)
        val deptType = ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)
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
        val managerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        ).relationId
        val isGroupBelongToProject = getGroupInfoList(
            resourceType = AuthResourceType.PROJECT.value,
            managerId = managerId
        ).map { it.id }.contains(iamGroupId)
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
        val userType = ManagerScopesEnum.getType(ManagerScopesEnum.USER)
        val deptType = ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)
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
        val members = mutableListOf<String>()
        val nowTimestamp = System.currentTimeMillis() / 1000
        groupMemberInfoList.forEach { memberInfo ->
            if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
                memberInfo.expiredAt > nowTimestamp) {
                members.add(memberInfo.id)
            }
        }
        return BkAuthGroupAndUserList(
            displayName = groupInfo.name,
            roleId = groupInfo.id,
            roleName = groupInfo.name,
            userIdList = members.toSet().toList(),
            type = ""
        )
    }

    override fun roleCodeToIamGroupId(
        projectCode: String,
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
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
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
        logger.info("renewal group member|$userId|$projectCode|$resourceType|$groupId")
        val managerMemberGroupDTO = GroupMemberRenewApplicationDTO.builder()
            .groupIds(listOf(groupId))
            .expiredAt(memberRenewalDTO.expiredAt)
            .reason("renewal user group")
            .applicant(userId).build()
        iamV2ManagerService.renewalRoleGroupMemberApplication(managerMemberGroupDTO)
        return true
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

    override fun batchRenewalGroupMembers(
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
        renewalIamGroupMembers(
            groupId = groupId,
            members = listOf(ManagerMember(targetMember.type, targetMember.id)),
            expiredAt = finalExpiredAt
        )
        authResourceGroupMemberDao.update(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = groupId,
            expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(expiredAt),
            memberId = targetMember.id
        )
    }

    private fun isNeedToRenewal(expiredAt: Long): Boolean {
        return expiredAt < PERMANENT_EXPIRED_TIME
    }

    override fun batchDeleteResourceGroupMembers(
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

    override fun deleteIamGroupMembers(
        groupId: Int,
        type: String,
        memberIds: List<String>
    ): Boolean {
        val membersOfNeedToDelete = if (type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
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

    private fun deleteTask(
        projectCode: String,
        groupId: Int,
        removeMemberDTO: GroupMemberCommonConditionReq,
        expiredAt: Long
    ) {
        val targetMember = removeMemberDTO.targetMember
        logger.info("delete group member $projectCode|$groupId|$targetMember")
        deleteIamGroupMembers(
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

    override fun batchHandoverGroupMembers(
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

    override fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): BatchOperateGroupMemberCheckVo {
        logger.info("batch operate group member check|$userId|$projectCode|$batchOperateType|$conditionReq")
        // 获取用户加入的用户组
        val (groupIdsOfDirectJoined, groupInfoIdsOfTemplateJoined) = getGroupIdsByCondition(
            projectCode = projectCode,
            commonCondition = conditionReq
        )
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

        addIamGroupMember(
            groupId = groupId,
            members = members,
            expiredAt = finalExpiredAt
        )
        deleteIamGroupMembers(
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
        val groupIds = getGroupIdsByCondition(
            projectCode = projectCode,
            commonCondition = conditionReq
        ).first
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

    private fun getGroupIdsByCondition(
        projectCode: String,
        commonCondition: GroupMemberCommonConditionReq
    ): Pair<List<Int>, List<Int>> /*直接加入，模板加入*/ {
        val finalResourceGroupMembers = mutableListOf<AuthResourceGroupMember>()
        with(commonCondition) {
            val resourceGroupMembersByCondition = when {
                // 全选
                allSelection -> {
                    listResourceGroupMembers(
                        projectCode = projectCode,
                        memberId = commonCondition.targetMember.id
                    ).second
                }
                // 全选某些资源类型用户组
                resourceTypes.isNotEmpty() -> {
                    resourceTypes.flatMap { resourceType ->
                        listResourceGroupMembers(
                            projectCode = projectCode,
                            memberId = commonCondition.targetMember.id,
                            resourceType = resourceType
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

            // Select specific groups individually
            if (groupIds.isNotEmpty()) {
                val resourceGroupMembersOfSelect = listResourceGroupMembers(
                    projectCode = projectCode,
                    memberId = commonCondition.targetMember.id,
                    iamGroupIds = groupIds
                ).second
                finalResourceGroupMembers.addAll(resourceGroupMembersOfSelect)
            }

            val (groupIdsOfDirectJoined, groupInfoIdsOfTemplateJoined) = finalResourceGroupMembers.partition {
                it.memberType != ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE)
            }.run {
                first.map { it.iamGroupId }.toMutableList() to second.map { it.iamGroupId }.toMutableList()
            }

            // When batch removing, if the user is the only manager of the group, ignore and do not transfer
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
            return Pair(groupIdsOfDirectJoined, groupInfoIdsOfTemplateJoined)
        }
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

    // 查询成员所在资源用户组列表，直接加入+通过用户组（模板）加入
    @Suppress("LongParameterList")
    override fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        start: Int?,
        limit: Int?
    ): Pair<Long, List<AuthResourceGroupMember>> {
        // 获取用户加入的项目级用户组模板ID
        val iamTemplateIds = listProjectMemberGroupTemplateIds(
            projectCode = projectCode,
            memberId = memberId
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
            maxExpiredAt = maxExpiredTime
        )[resourceType] ?: 0L
        val resourceGroupMembers = authResourceGroupMemberDao.listMemberGroupDetail(
            dslContext = dslContext,
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredTime,
            maxExpiredAt = maxExpiredTime,
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

    private fun MutableList<ManagerMember>.removeDepartedMembers(): List<ManagerMember> {
        val userMemberIds = this.filter { it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER) }.map { it.id }
        if (userMemberIds.isEmpty()) return this
        // 获取离职的人员
        val departedMembers = deptService.listDepartedMembers(
            memberIds = userMemberIds
        )
        return this.filterNot {
            it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER) &&
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

        // 永久过期时间
        private const val PERMANENT_EXPIRED_TIME = 4102444800000L
    }
}
