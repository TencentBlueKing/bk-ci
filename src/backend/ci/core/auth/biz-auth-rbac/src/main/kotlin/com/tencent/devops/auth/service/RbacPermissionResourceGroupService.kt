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
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.AUTH_GROUP_MEMBER_EXPIRED_DESC
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.enum.GroupMemberStatus
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class RbacPermissionResourceGroupService @Autowired constructor(
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val permissionSubsetManagerService: PermissionSubsetManagerService,
    private val permissionResourceService: PermissionResourceService,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService
) : PermissionResourceGroupService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupService::class.java)
    }

    override fun listGroup(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): List<IamGroupInfoVo> {
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return if (resourceType == AuthResourceType.PROJECT.value) {
            permissionGradeManagerService.listGroup(resourceInfo.relationId)
        } else {
            permissionSubsetManagerService.listGroup(resourceInfo.relationId)
        }
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
                        MessageCodeUtil.getCodeLanMessage(
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
                    expiredDisplay = "",
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
        val managerMember = ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), userId)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
            .members(listOf(managerMember))
            .expiredAt(memberRenewalDTO.expiredAt)
            .build()
        iamV2ManagerService.renewalRoleGroupMemberV2(
            groupId,
            managerMemberGroupDTO
        )
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
        if (!permissionResourceService.hasManagerPermission(
                userId = userId,
                projectId = projectId,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectId
            )) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
        iamV2ManagerService.deleteRoleGroupV2(groupId)
        return true
    }
}
