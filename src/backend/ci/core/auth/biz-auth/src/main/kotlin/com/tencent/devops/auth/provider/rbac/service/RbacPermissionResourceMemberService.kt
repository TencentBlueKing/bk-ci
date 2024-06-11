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
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.constant.ProjectMessageCode
import org.apache.commons.lang3.RandomUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RbacPermissionResourceMemberService constructor(
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceGroupDao: AuthResourceGroupDao,
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
        members?.forEach {
            val shouldAddUserToGroup = shouldAddUserToGroup(
                groupUserMap = groupUserMap,
                groupDepartmentSet = groupDepartmentSet,
                member = it
            )
            if (shouldAddUserToGroup) {
                iamMemberInfos.add(ManagerMember(userType, it))
            }
        }

        departments?.forEach {
            if (!groupDepartmentSet.contains(it)) {
                iamMemberInfos.add(ManagerMember(deptType, it))
            }
        }
        logger.info("batch add project user:|$projectCode|$iamGroupId|$expiredTime|$iamMemberInfos")
        if (iamMemberInfos.isNotEmpty()) {
            val managerMemberGroup =
                ManagerMemberGroupDTO.builder().members(iamMemberInfos).expiredAt(expiredTime).build()
            iamV2ManagerService.createRoleGroupMemberV2(iamGroupId, managerMemberGroup)
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
        if (!members.isNullOrEmpty()) {
            iamV2ManagerService.deleteRoleGroupMemberV2(iamGroupId, userType, members.joinToString(","))
        }
        if (!departments.isNullOrEmpty()) {
            iamV2ManagerService.deleteRoleGroupMemberV2(iamGroupId, deptType, departments.joinToString(","))
        }
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
        resourceCode: String
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
        val expectAutoExpiredAt = currentTime + AUTO_VALID_EXPIRED_AT
        val autoRenewalMembers = mutableSetOf<String>()
        resourceGroupInfoList.forEach group@{ resourceGroup ->
            val iamGroupId = resourceGroup.relationId.toInt()
            val pageInfoDTO = V2PageInfoDTO().apply {
                pageSize = 1000
                page = 1
            }
            val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(iamGroupId, pageInfoDTO).results
            groupMemberInfoList.forEach member@{ member ->
                // 已过期或者要半年后才过期的,不自动过期
                if (member.expiredAt < currentTime ||
                    member.expiredAt > expectAutoExpiredAt
                ) return@member

                // 自动续期时间由半年+随机天数,防止同一时间同时过期
                val expiredTime = currentTime + AUTO_RENEWAL_EXPIRED_AT +
                    TimeUnit.DAYS.toSeconds(RandomUtils.nextLong(0, 180))
                val managerMemberGroup =
                    ManagerMemberGroupDTO.builder().members(listOf(ManagerMember(member.type, member.id)))
                        .expiredAt(expiredTime).build()
                autoRenewalMembers.add(member.id)
                try {
                    iamV2ManagerService.createRoleGroupMemberV2(iamGroupId, managerMemberGroup)
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

    override fun deleteGroupMember(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group member|$userId|$projectCode|$resourceType|$groupId")
        iamV2ManagerService.deleteRoleGroupMemberV2(
            groupId,
            ManagerScopesEnum.getType(ManagerScopesEnum.USER),
            userId
        )
        return true
    }

    override fun addGroupMember(
        userId: String,
        /*user 或 department*/
        memberType: String,
        expiredAt: Long,
        groupId: Int
    ): Boolean {
        val managerMember = ManagerMember(memberType, userId)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
            .members(listOf(managerMember))
            .expiredAt(expiredAt)
            .build()
        iamV2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroupDTO)
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        // 有效的过期时间,在30天内就是有效的
        private const val VALID_EXPIRED_AT = 30L

        // 自动续期有效的过期时间,在180天以上就不需要自动续期
        private val AUTO_VALID_EXPIRED_AT = TimeUnit.DAYS.toSeconds(180)

        // 自动续期默认180天
        private val AUTO_RENEWAL_EXPIRED_AT = TimeUnit.DAYS.toSeconds(180)

        private val executorService = Executors.newFixedThreadPool(10)
    }
}
