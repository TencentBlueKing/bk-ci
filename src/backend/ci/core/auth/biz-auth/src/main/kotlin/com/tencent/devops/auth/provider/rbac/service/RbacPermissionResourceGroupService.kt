/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.bk.sdk.iam.dto.InstancesDTO
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
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.enum.GroupMemberStatus
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("LongParameterList")
class RbacPermissionResourceGroupService @Autowired constructor(
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val permissionSubsetManagerService: PermissionSubsetManagerService,
    private val permissionProjectService: PermissionProjectService,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val v2ManagerService: V2ManagerService,
    private val rbacCacheService: RbacCacheService,
    private val monitorSpaceService: AuthMonitorSpaceService,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao
) : PermissionResourceGroupService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    @Value("\${monitor.register:false}")
    private val registerMonitor: Boolean = false

    @Value("\${monitor.iamSystem:}")
    private val monitorSystemId = ""

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupService::class.java)
        private const val MAX_GROUP_NAME_LENGTH = 32
        private const val MIN_GROUP_NAME_LENGTH = 5

        // 毫秒转换
        private const val MILLISECOND = 1000
    }

    override fun listGroup(
        projectId: String,
        resourceType: String,
        resourceCode: String,
        page: Int,
        pageSize: Int
    ): Pagination<IamGroupInfoVo> {
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val validPage = PageUtil.getValidPage(page)
        val validPageSize = PageUtil.getValidPageSize(pageSize)
        val iamGroupInfoList = if (resourceType == AuthResourceType.PROJECT.value) {
            val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
            val pageInfoDTO = V2PageInfoDTO()
            pageInfoDTO.page = page
            pageInfoDTO.pageSize = pageSize
            val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
                resourceInfo.relationId,
                searchGroupDTO,
                pageInfoDTO
            )
            iamGroupInfoList.results
        } else {
            permissionSubsetManagerService.listGroup(
                subsetManagerId = resourceInfo.relationId,
                page = validPage,
                pageSize = validPageSize
            )
        }
        val resourceGroupMap = authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).associateBy { it.relationId.toInt() }
        val iamGroupInfoVoList = iamGroupInfoList.map {
            val resourceGroup = resourceGroupMap[it.id]
            val defaultGroup = resourceGroup?.defaultGroup ?: false
            // 默认组名需要支持国际化
            val groupName = if (defaultGroup) {
                I18nUtil.getCodeLanMessage(
                    messageCode = "${resourceGroup!!.resourceType}.${resourceGroup.groupCode}" +
                        AuthI18nConstants.AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX,
                    defaultMessage = resourceGroup.groupName
                )
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
                departmentCount = it.departmentCount
            )
        }.sortedBy { it.groupId }
        return Pagination(
            hasNext = iamGroupInfoVoList.size == pageSize,
            records = iamGroupInfoVoList
        )
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
                            AUTH_GROUP_MEMBER_EXPIRED_DESC,
                            "expired"
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

    override fun getGroupPolicies(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): List<IamGroupPoliciesVo> {
        logger.info("get group policies|$projectId|$resourceType|$groupId")
        return permissionGroupPoliciesService.getGroupPolices(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            groupId = groupId
        )
    }

    override fun deleteGroup(
        userId: String?,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group|$userId|$projectId|$resourceType|$groupId")
        if (userId != null) {
            checkProjectManager(
                userId = userId,
                projectId = projectId
            )
        }
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
            authResourceGroupDao.deleteByIds(
                dslContext = dslContext,
                ids = listOf(authResourceGroup.id)
            )
        }
        return true
    }

    private fun checkProjectManager(
        userId: String,
        projectId: String
    ) {
        val hasProjectManagePermission = permissionProjectService.checkProjectManager(
            userId = userId,
            projectCode = projectId
        )
        if (!hasProjectManagePermission) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
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

        return createProjectGroupToIam(
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            relationId = projectInfo.relationId.toInt(),
            groupCode = "custom",
            groupName = groupAddDTO.groupName,
            description = groupAddDTO.groupDesc
        )
    }

    private fun createProjectGroupToIam(
        projectCode: String,
        projectName: String,
        relationId: Int,
        groupCode: String,
        groupName: String,
        description: String
    ): Int {
        val managerRoleGroup = ManagerRoleGroup(groupName, description, false)
        val managerRoleGroupDTO = ManagerRoleGroupDTO.builder()
            .groups(listOf(managerRoleGroup))
            .createAttributes(false)
            .syncSubjectTemplate(true)
            .build()
        val iamGroupId = iamV2ManagerService.batchCreateRoleGroupV2(relationId, managerRoleGroupDTO)
        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode,
            groupCode = groupCode,
            groupName = groupName,
            defaultGroup = false,
            relationId = iamGroupId.toString()
        )
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
        checkProjectManager(
            userId = userId,
            projectId = projectId
        )
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

    override fun getGroupPermissionDetail(groupId: Int): Map<String, List<GroupPermissionDetailVo>> {
        val groupPermissionMap = mutableMapOf<String, List<GroupPermissionDetailVo>>()
        groupPermissionMap[I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_DEVOPS_NAME)] =
            getGroupPermissionDetailBySystem(systemId, groupId)
        if (registerMonitor) {
            val monitorGroupPermissionDetail = getGroupPermissionDetailBySystem(monitorSystemId, groupId)
            if (monitorGroupPermissionDetail.isNotEmpty()) {
                groupPermissionMap[I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_MONITOR_NAME)] =
                    getGroupPermissionDetailBySystem(monitorSystemId, groupId)
            }
        }
        return groupPermissionMap
    }

    override fun createProjectGroupByGroupCode(
        projectId: String,
        groupCode: String
    ): Boolean {
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val groupConfig = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            groupCode = groupCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.DEFAULT_GROUP_CONFIG_NOT_FOUND,
            defaultMessage = "group($groupCode) config not exist"
        )
        val resourceGroupInfo = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            groupCode = groupConfig.groupCode
        )
        if (resourceGroupInfo != null) {
            return false
        }
        val iamGroupId = createProjectGroupToIam(
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            relationId = projectInfo.relationId.toInt(),
            groupCode = groupConfig.groupCode,
            groupName = groupConfig.groupName,
            description = groupConfig.description
        )
        permissionGroupPoliciesService.grantGroupPermission(
            authorizationScopesStr = groupConfig.authorizationScopes,
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            resourceType = groupConfig.resourceType,
            groupCode = groupConfig.groupCode,
            iamResourceCode = projectId,
            resourceName = projectInfo.resourceName,
            iamGroupId = iamGroupId
        )
        return true
    }

    private fun getGroupPermissionDetailBySystem(iamSystemId: String, groupId: Int): List<GroupPermissionDetailVo> {
        val iamGroupPermissionDetailList = try {
            v2ManagerService.getGroupPermissionDetail(groupId, iamSystemId)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_GROUP_PERMISSION_DETAIL_FAIL,
                params = arrayOf(groupId.toString()),
                defaultMessage = "Failed to get group($groupId) permission info"
            )
        }
        return iamGroupPermissionDetailList.map { detail ->
            val relatedResourceTypesDTO = detail.resourceGroups[0].relatedResourceTypesDTO[0]
            // 将resourceType转化为对应的资源类型名称
            buildRelatedResourceTypesName(
                iamSystemId = iamSystemId,
                instancesDTO = relatedResourceTypesDTO.condition[0].instances[0]
            )
            val relatedResourceInfo = RelatedResourceInfo(
                type = relatedResourceTypesDTO.type,
                name = I18nUtil.getCodeLanMessage(
                    relatedResourceTypesDTO.type + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
                ),
                instances = relatedResourceTypesDTO.condition[0].instances[0]
            )
            val actionName = if (iamSystemId == monitorSystemId) {
                monitorSpaceService.getMonitorActionName(action = detail.id)
            } else {
                rbacCacheService.getActionInfo(action = detail.id).actionName
            }
            GroupPermissionDetailVo(
                actionId = detail.id,
                name = actionName!!,
                relatedResourceInfo = relatedResourceInfo
            )
        }.sortedBy { it.relatedResourceInfo.type }
    }

    private fun buildRelatedResourceTypesName(iamSystemId: String, instancesDTO: InstancesDTO) {
        instancesDTO.let {
            val resourceTypeName = if (iamSystemId == systemId) {
                rbacCacheService.getResourceTypeInfo(it.type).name
            } else {
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_MONITOR_SPACE)
            }
            it.name = resourceTypeName
            it.path.forEach { element1 ->
                element1.forEach { element2 ->
                    element2.typeName = resourceTypeName
                }
            }
        }
    }
}
