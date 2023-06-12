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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.dto.GroupMemberRenewApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode.AUTH_GROUP_MEMBER_EXPIRED_DESC
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_DEFAULT_GROUP_DELETE_FAIL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_DEFAULT_GROUP_RENAME_FAIL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_GROUP_NAME_TO_LONG
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_GROUP_NAME_TO_SHORT
import com.tencent.devops.auth.constant.AuthMessageCode.GROUP_EXIST
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.enum.GroupMemberStatus
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList")
class RbacPermissionResourceGroupService @Autowired constructor(
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val permissionSubsetManagerService: PermissionSubsetManagerService,
    private val permissionResourceService: PermissionResourceService,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao
) : PermissionResourceGroupService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupService::class.java)
        private const val MAX_GROUP_NAME_LENGTH = 32
        private const val MIN_GROUP_NAME_LENGTH = 5
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
            permissionGradeManagerService.listGroup(
                gradeManagerId = resourceInfo.relationId,
                page = validPage,
                pageSize = validPageSize
            )
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
            IamGroupInfoVo(
                managerId = resourceInfo.relationId.toInt(),
                defaultGroup = resourceGroupMap[it.id]?.defaultGroup ?: false,
                groupId = it.id,
                name = it.name,
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
                    expiredDisplay = expiredDisplay
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

    override fun renewal(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Boolean {
        logger.info("renewal group member|$userId|$projectId|$resourceType|$groupId")
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
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group member|$userId|$projectId|$resourceType|$groupId")
        iamV2ManagerService.deleteRoleGroupMemberV2(
            groupId,
            ManagerScopesEnum.getType(ManagerScopesEnum.USER),
            userId
        )
        return true
    }

    override fun deleteGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group|$userId|$projectId|$resourceType|$groupId")
        permissionResourceService.hasManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
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

    override fun rename(
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
        permissionResourceService.hasManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
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
}
