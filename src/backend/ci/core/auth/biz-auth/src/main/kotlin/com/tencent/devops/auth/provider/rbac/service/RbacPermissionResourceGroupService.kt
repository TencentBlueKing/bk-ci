/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.AUTH_GROUP_MEMBER_EXPIRED_DESC
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_DEFAULT_GROUP_DELETE_FAIL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_DEFAULT_GROUP_RENAME_FAIL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_GROUP_NAME_TO_LONG
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_GROUP_NAME_TO_SHORT
import com.tencent.devops.auth.constant.AuthMessageCode.GROUP_EXIST
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.dto.ListGroupConditionDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.enum.GroupMemberStatus
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.service.BkInternalPermissionCache
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.lock.GroupAndPermissionsByGroupCodeCreateLock
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.enums.GroupType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList")
class RbacPermissionResourceGroupService @Autowired constructor(
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val authResourceDao: AuthResourceDao,
    private val resourceGroupSyncService: PermissionResourceGroupSyncService,
    private val redisOperation: RedisOperation,
    private val deptService: DeptService
) : PermissionResourceGroupService {
    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupService::class.java)
        private const val MAX_GROUP_NAME_LENGTH = 32
        private const val MIN_GROUP_NAME_LENGTH = 5
        private const val FIRST_PAGE = 1
        private const val CUSTOM_GROUP_CODE = "custom"
    }

    override fun listGroup(
        userId: String,
        listGroupConditionDTO: ListGroupConditionDTO
    ): Pagination<IamGroupInfoVo> {
        with(listGroupConditionDTO) {
            val resourceInfo = authResourceService.get(
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )

            val iamGroupInfoList = if (resourceType == AuthResourceType.PROJECT.value) {
                val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
                val pageInfoDTO = V2PageInfoDTO()
                pageInfoDTO.page = page
                pageInfoDTO.pageSize = pageSize
                iamV2ManagerService.getGradeManagerRoleGroupV2(
                    resourceInfo.relationId,
                    searchGroupDTO,
                    pageInfoDTO
                ).results
            } else {
                val pageInfoDTO = V2PageInfoDTO()
                val validPage = PageUtil.getValidPage(page)
                val validPageSize = PageUtil.getValidPageSize(pageSize)
                pageInfoDTO.page = validPage
                pageInfoDTO.pageSize = validPageSize
                iamV2ManagerService.getSubsetManagerRoleGroup(
                    resourceInfo.relationId.toInt(),
                    pageInfoDTO
                ).results
            }
            val resourceGroupMap = authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            ).associateBy { it.relationId }
            val iamGroupInfoVoList = iamGroupInfoList.map {
                val resourceGroup = resourceGroupMap[it.id]
                val defaultGroup = resourceGroup?.defaultGroup ?: false
                // 默认组名需要支持国际化
                val groupName =
                    if ((resourceType == ResourceTypeId.PROJECT && defaultGroup) ||
                        resourceType != ResourceTypeId.PROJECT
                    ) {
                        getI18nGroupName(resourceGroup!!)
                    } else {
                        it.name
                    }
                IamGroupInfoVo(
                    managerId = resourceInfo.relationId.toInt(),
                    defaultGroup = defaultGroup,
                    groupId = it.id,
                    name = groupName,
                    displayName = it.name,
                    userCount = it.userCount,
                    departmentCount = it.departmentCount,
                    templateCount = it.templateCount
                )
            }.toMutableList().plusAllProjectMemberGroup(
                userId = userId,
                managerId = resourceInfo.relationId.toInt(),
                condition = listGroupConditionDTO
            ).sortedBy { it.groupId }
            return Pagination(
                hasNext = iamGroupInfoVoList.size == pageSize,
                records = iamGroupInfoVoList
            )
        }
    }

    private fun getI18nGroupName(resourceGroup: AuthResourceGroup): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = "${resourceGroup.resourceType}.${resourceGroup.groupCode}" +
                AuthI18nConstants.AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX,
            defaultMessage = resourceGroup.groupName
        )
    }

    private fun MutableList<IamGroupInfoVo>.plusAllProjectMemberGroup(
        userId: String,
        managerId: Int,
        condition: ListGroupConditionDTO
    ): List<IamGroupInfoVo> {
        val shouldPlusAllProjectMemberGroup =
            condition.page == FIRST_PAGE &&
                condition.resourceType == AuthResourceType.PROJECT.value &&
                condition.getAllProjectMembersGroup

        if (shouldPlusAllProjectMemberGroup) {
            val projectMemberCount = authResourceGroupMemberDao.countProjectMember(
                dslContext = dslContext,
                projectCode = condition.projectId
            )
            val userCount = projectMemberCount[MemberType.USER.type] ?: 0
            val departmentCount = projectMemberCount[MemberType.DEPARTMENT.type] ?: 0
            val allProjectMemberGroup = IamGroupInfoVo(
                managerId = managerId,
                defaultGroup = true,
                groupId = 0,
                name = MessageUtil.getMessageByLocale(
                    AuthI18nConstants.BK_ALL_PROJECT_MEMBERS_GROUP,
                    I18nUtil.getLanguage(userId)
                ),
                displayName = MessageUtil.getMessageByLocale(
                    AuthI18nConstants.BK_ALL_PROJECT_MEMBERS_GROUP,
                    I18nUtil.getLanguage(userId)
                ),
                userCount = userCount,
                departmentCount = departmentCount,
                projectMemberGroup = true
            )
            this.add(0, allProjectMemberGroup)
        }
        return this
    }

    override fun listUserBelongGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): List<IamGroupMemberInfoVo> {
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val iamGroupInfos =
            iamV2ManagerService.getSubsetManagerRoleGroup(resourceInfo.relationId.toInt(), pageInfoDTO)
        val iamGroupInfoMap = iamGroupInfos.results.associateBy { it.id }
        val verifyResult =
            iamV2ManagerService.verifyGroupValidMember(userId, iamGroupInfoMap.keys.joinToString(","))
        return verifyResult.map { (iamGroupId, result) ->
            if (result.belong) {
                val createTime = DateTimeUtil.toDateTime(
                    dateTime = DateTimeUtil.stringToLocalDateTime(
                        dateTimeStr = result.createdAt.replace("Z", " UTC"),
                        formatStr = DateTimeUtil.YYYY_MM_DD_T_HH_MM_SSZ
                    )
                )
                val expiredAt = result.expiredAt * 1000
                val between = expiredAt - System.currentTimeMillis()
                val (status, expiredDisplay) = if (between <= 0) {
                    Pair(
                        GroupMemberStatus.EXPIRED.name,
                        I18nUtil.getCodeLanMessage(
                            AUTH_GROUP_MEMBER_EXPIRED_DESC
                        )
                    )
                } else {
                    Pair(GroupMemberStatus.NORMAL.name, DateTimeUtil.formatDay(between))
                }
                IamGroupMemberInfoVo(
                    userId = userId,
                    groupId = iamGroupId,
                    groupName = iamGroupInfoMap[iamGroupId]?.name ?: "",
                    createdTime = createTime,
                    status = status,
                    expiredAt = expiredAt,
                    expiredDisplay = expiredDisplay,
                    directAdded = result.directAdded.toBoolean()
                )
            } else {
                val status = GroupMemberStatus.NOT_JOINED.name
                IamGroupMemberInfoVo(
                    userId = userId,
                    groupId = iamGroupId,
                    groupName = iamGroupInfoMap[iamGroupId]?.name ?: "",
                    createdTime = "",
                    status = status,
                    expiredAt = 0L,
                    expiredDisplay = ""
                )
            }
        }
    }

    override fun listIamGroupIdsByGroupName(
        projectId: String,
        groupName: String
    ): List<Int> {
        return authResourceGroupDao.listIamGroupIdsByConditions(
            dslContext = dslContext,
            projectCode = projectId,
            groupName = groupName
        )
    }

    override fun deleteGroup(
        userId: String?,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group|$userId|$projectId|$resourceType|$groupId")
        val authResourceGroup = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupId = groupId.toString()
        )
        if (authResourceGroup != null && authResourceGroup.defaultGroup) {
            throw ErrorCodeException(
                errorCode = ERROR_DEFAULT_GROUP_DELETE_FAIL,
                defaultMessage = "default group cannot be deleted"
            )
        }
        iamV2ManagerService.deleteRoleGroupV2(groupId)
        // 迁移的用户组,非默认的也会保存,删除时也应该删除
        if (authResourceGroup != null) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                authResourceGroupDao.deleteByIds(
                    dslContext = context,
                    ids = listOf(authResourceGroup.id)
                )
                val memberIds = authResourceGroupMemberDao.listResourceGroupMember(
                    dslContext = dslContext,
                    projectCode = projectId,
                    iamGroupId = groupId
                ).map { it.memberId }
                BkInternalPermissionCache.batchInvalidateProjectUserGroups(
                    projectCode = projectId,
                    userIds = memberIds
                )
                authResourceGroupMemberDao.deleteByIamGroupId(
                    dslContext = context,
                    projectCode = projectId,
                    iamGroupId = groupId
                )
            }
            permissionResourceGroupPermissionService.deleteByGroupIds(
                projectCode = projectId,
                iamGroupIds = listOf(groupId)
            )
        }
        return true
    }

    override fun createGroup(
        projectId: String,
        groupAddDTO: GroupAddDTO
    ): Int {
        logger.info("create group|$projectId|$groupAddDTO")
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val iamGroupId = createGroupToIam(
            managerId = projectInfo.relationId.toInt(),
            resourceType = AuthResourceType.PROJECT.value,
            groupName = groupAddDTO.groupName,
            description = groupAddDTO.groupDesc
        )
        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            resourceName = projectInfo.resourceName,
            iamResourceCode = projectId,
            groupCode = CUSTOM_GROUP_CODE,
            groupName = groupAddDTO.groupName,
            defaultGroup = false,
            relationId = iamGroupId.toString()
        )
        return iamGroupId
    }

    private fun createGroupToIam(
        resourceType: String,
        managerId: Int,
        groupName: String,
        description: String
    ): Int {
        val iamGroupId = if (resourceType == AuthResourceType.PROJECT.value) {
            val managerRoleGroup = ManagerRoleGroup(groupName, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder()
                .groups(listOf(managerRoleGroup))
                .createAttributes(false)
                .syncSubjectTemplate(true)
                .build()
            iamV2ManagerService.batchCreateRoleGroupV2(managerId, managerRoleGroupDTO)
        } else {
            val managerRoleGroup = ManagerRoleGroup(groupName, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            iamV2ManagerService.batchCreateSubsetRoleGroup(managerId, managerRoleGroupDTO)
        }
        return iamGroupId
    }

    override fun renameGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        renameGroupDTO: RenameGroupDTO
    ): Boolean {
        logger.info("rename group name|$userId|$projectId|$resourceType|$groupId|${renameGroupDTO.groupName}")
        if (renameGroupDTO.groupName.length > MAX_GROUP_NAME_LENGTH) {
            throw ErrorCodeException(
                errorCode = ERROR_GROUP_NAME_TO_LONG,
                defaultMessage = "group name cannot exceed 32 characters"
            )
        }
        if (renameGroupDTO.groupName.length < MIN_GROUP_NAME_LENGTH) {
            throw ErrorCodeException(
                errorCode = ERROR_GROUP_NAME_TO_SHORT,
                defaultMessage = "group name cannot be less than 5 characters"
            )
        }
        val authResourceGroup = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupId = groupId.toString()
        )
        if (authResourceGroup != null && authResourceGroup.defaultGroup) {
            throw ErrorCodeException(
                errorCode = ERROR_DEFAULT_GROUP_RENAME_FAIL,
                defaultMessage = "default group cannot be rename"
            )
        }
        checkDuplicateGroupName(projectId, renameGroupDTO.groupName)
        val managerRoleGroup = ManagerRoleGroup()
        managerRoleGroup.name = renameGroupDTO.groupName
        iamV2ManagerService.updateRoleGroupV2(groupId, managerRoleGroup)
        return true
    }

    private fun checkDuplicateGroupName(
        projectId: String,
        groupName: String
    ) {
        // 校验用户组是否存在
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val searchGroupDTO = SearchGroupDTO
            .builder()
            .name(groupName)
            .build()
        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE
        v2PageInfoDTO.page = PageUtil.DEFAULT_PAGE
        val gradeManagerRoleGroupList =
            iamV2ManagerService.getGradeManagerRoleGroupV2(projectInfo.relationId, searchGroupDTO, v2PageInfoDTO)
        if (gradeManagerRoleGroupList.results.isNotEmpty() &&
            gradeManagerRoleGroupList.results.map { it.name }.contains(groupName)
        ) {
            throw ErrorCodeException(
                errorCode = GROUP_EXIST,
                defaultMessage = "group name already exists"
            )
        }
    }

    override fun createGroupAndPermissionsByGroupCode(
        projectId: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        groupName: String?,
        groupDesc: String?
    ): Int {
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val groupConfig = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = groupCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.DEFAULT_GROUP_CONFIG_NOT_FOUND,
            defaultMessage = "group($groupCode) config not exist"
        )
        val (finalGroupName, finalGroupCode, description) = if (groupName != null) {
            Triple(groupName, CUSTOM_GROUP_CODE, groupDesc ?: groupName)
        } else {
            Triple(groupConfig.groupName, groupConfig.groupCode, groupConfig.description)
        }
        GroupAndPermissionsByGroupCodeCreateLock(
            redisOperation = redisOperation,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = finalGroupCode
        ).use { lock ->
            lock.lock()
            val resourceGroupInfo = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = finalGroupCode,
                groupName = finalGroupName
            )
            if (resourceGroupInfo != null) {
                return resourceGroupInfo.relationId.toInt()
            }
            val iamGroupId = createGroupToIam(
                resourceType = resourceType,
                managerId = resourceInfo.relationId.toInt(),
                groupName = finalGroupName,
                description = description
            )
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceInfo.resourceName,
                iamResourceCode = resourceInfo.iamResourceCode,
                groupCode = finalGroupCode,
                groupName = finalGroupName,
                defaultGroup = false,
                relationId = iamGroupId.toString()
            )
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = groupConfig.authorizationScopes,
                projectCode = projectId,
                projectName = projectInfo.resourceName,
                resourceType = groupConfig.resourceType,
                groupCode = finalGroupCode,
                iamResourceCode = resourceInfo.iamResourceCode,
                resourceName = resourceInfo.resourceName,
                iamGroupId = iamGroupId
            )
            return iamGroupId
        }
    }

    override fun createCustomGroupAndPermissions(
        projectId: String,
        customGroupCreateReq: CustomGroupCreateReq
    ): Int {
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val resourceGroupInfo = authResourceGroupDao.getByGroupName(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            groupName = customGroupCreateReq.groupName
        )
        if (resourceGroupInfo != null)
            return resourceGroupInfo.relationId.toInt()
        val authorizationScopes = permissionResourceGroupPermissionService.buildProjectPermissions(
            projectCode = projectInfo.projectCode,
            projectName = projectInfo.resourceName,
            actions = customGroupCreateReq.actions
        )
        val iamGroupId = createGroupToIam(
            resourceType = AuthResourceType.PROJECT.value,
            managerId = projectInfo.relationId.toInt(),
            groupName = customGroupCreateReq.groupName,
            description = customGroupCreateReq.groupDesc
        )
        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            resourceName = projectInfo.resourceName,
            iamResourceCode = projectId,
            groupCode = CUSTOM_GROUP_CODE,
            groupName = customGroupCreateReq.groupName,
            defaultGroup = false,
            relationId = iamGroupId.toString()
        )
        permissionResourceGroupPermissionService.grantGroupPermission(
            authorizationScopesStr = authorizationScopes,
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            resourceType = AuthResourceType.PROJECT.value,
            groupCode = CUSTOM_GROUP_CODE,
            iamResourceCode = projectId,
            resourceName = projectInfo.resourceName,
            iamGroupId = iamGroupId
        )
        return iamGroupId
    }

    override fun getByGroupCode(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: BkAuthGroup
    ): AuthResourceGroup? {
        return authResourceGroupDao.getByGroupCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = groupCode.value
        )
    }

    override fun listProjectMemberGroupTemplateIds(
        projectCode: String,
        memberId: String
    ): List<String> {
        // 获取用户的所属组织
        val memberDeptInfos = deptService.getUserInfo(memberId)?.deptInfo?.let {
            if (it.isNotEmpty()) {
                deptService.getUserDeptInfo(memberId).toList()
            } else {
                emptyList()
            }
        } ?: emptyList()
        // 查询项目下包含该成员及所属组织的用户组列表
        val projectGroupIds = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            memberIds = memberDeptInfos + memberId
        ).map { it.iamGroupId.toString() }
        // 通过项目组ID获取人员模板ID
        return authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = projectGroupIds
        ).filter { it.iamTemplateId != null }
            .map { it.iamTemplateId.toString() }
    }

    override fun syncManagerGroup(
        projectCode: String,
        managerId: Int,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String
    ): Boolean {
        val resourceManageGroupInfo = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = DefaultGroupType.MANAGER.value
        )
        if (resourceManageGroupInfo != null) {
            return true
        }
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val iamGroupInfoList = if (resourceType == AuthResourceType.PROJECT.value) {
            val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
            iamV2ManagerService.getGradeManagerRoleGroupV2(
                managerId.toString(),
                searchGroupDTO,
                pageInfoDTO
            )
        } else {
            iamV2ManagerService.getSubsetManagerRoleGroup(
                managerId,
                pageInfoDTO
            )
        }
        iamGroupInfoList.results.forEach { iamGroupInfo ->
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = iamResourceCode,
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = iamGroupInfo.name,
                defaultGroup = true,
                relationId = iamGroupInfo.id.toString()
            )
            // 同步拥有者组里面的成员
            resourceGroupSyncService.syncIamGroupMember(
                projectCode = projectCode,
                iamGroupId = iamGroupInfo.id
            )
            // 同步组权限
            permissionResourceGroupPermissionService.syncGroupPermissions(
                projectCode = projectCode,
                iamGroupId = iamGroupInfo.id
            )
        }
        return true
    }

    override fun deleteManagerDefaultGroup(
        userId: String,
        managerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        val records = authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }
        records.forEach {
            logger.info("delete subset manage default group|$managerId|${it.relationId}")
            iamV2ManagerService.deleteRoleGroupV2(it.relationId)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            authResourceDao.disable(
                dslContext = transactionContext,
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            authResourceGroupDao.deleteByIds(
                dslContext = transactionContext,
                ids = records.map { it.id!! }
            )
            val memberIds = authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = records.map { it.relationId }
            ).map { it.memberId }.distinct()
            BkInternalPermissionCache.batchInvalidateProjectUserGroups(
                projectCode = projectCode,
                userIds = memberIds
            )
            authResourceGroupMemberDao.deleteByIamGroupIds(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = records.map { it.relationId }
            )
        }
        permissionResourceGroupPermissionService.deleteByGroupIds(
            projectCode = projectCode,
            iamGroupIds = records.map { it.relationId }
        )
        return true
    }

    override fun modifyManagerDefaultGroup(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupType = GroupType.DEFAULT.value
        )
        defaultGroupConfigs.forEach { groupConfig ->
            authResourceGroupDao.update(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                groupCode = groupConfig.groupCode,
                groupName = groupConfig.groupName
            )
        }
        return true
    }
}
