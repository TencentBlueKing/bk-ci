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
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthDefaultGroupDao
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList")
class PermissionResourceGroupService(
    private val dslContext: DSLContext,
    private val authDefaultGroupDao: AuthDefaultGroupDao,
    private val iamV2ManagerService: V2ManagerService,
    private val permissionScopesService: PermissionScopesService,
    private val groupService: AuthGroupService
) {

    fun createDefaultGroup(
        subsetManagerId: Int,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ) {
        // createLocalManagerGroup(resourceType, subsetManagerId, userId, projectCode)
        val defaultGroups = authDefaultGroupDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            createMode = false
        )
        defaultGroups.filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }.forEach { defaultGroup ->
            val name = IamGroupUtils.buildSubsetManagerGroupName(
                resourceName = resourceName,
                groupName = defaultGroup.groupName
            )
            val description = IamGroupUtils.buildSubsetManagerGroupDescription(
                resourceName = resourceName,
                groupName = name,
                userId = userId
            )
            val managerRoleGroup = ManagerRoleGroup(name, description, true)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            val iamGroupId = iamV2ManagerService.batchCreateSubsetRoleGroup(subsetManagerId, managerRoleGroupDTO)
            grantGroupPermission(
                projectCode = projectCode,
                projectName = projectName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                groupCode = defaultGroup.groupCode,
                iamGroupId = iamGroupId
            )
            addGroupMember(userId = userId, iamGroupId = iamGroupId)
            /*groupService.createGroup(
                userId = userId,
                projectCode = projectCode,
                groupInfo = GroupDTO(
                    groupCode = defaultGroup.groupCode,
                    groupType = true,
                    groupName = name,
                    displayName = defaultGroup.groupName,
                    relationId = iamGroupId.toString()
                )
            )*/
        }
    }

    /**
     * 将管理员组添加到本地用户组列表
     */
    private fun createLocalManagerGroup(
        resourceType: String,
        subsetManagerId: Int,
        userId: String,
        projectCode: String
    ) {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 1
        val managerDefaultGroup = authDefaultGroupDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        )!!
        iamV2ManagerService.getSubsetManagerRoleGroup(subsetManagerId, pageInfoDTO).results.forEach { iamGroup ->
            groupService.createGroup(
                userId = userId,
                projectCode = projectCode,
                groupInfo = GroupDTO(
                    groupCode = managerDefaultGroup.groupCode,
                    groupType = true,
                    groupName = iamGroup.name,
                    displayName = managerDefaultGroup.groupName,
                    relationId = iamGroup.id.toString()
                )
            )
        }
    }

    fun grantGroupPermission(
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        groupCode: String,
        iamGroupId: Int
    ) {
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildSubsetManagerGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
        authorizationScopes.forEach { authorizationScope ->
            iamV2ManagerService.grantRoleGroupV2(iamGroupId, authorizationScope)
        }
    }

    fun addGroupMember(
        userId: String,
        iamGroupId: Int
    ) {
        val groupMember = ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), userId)
        val groupMembers = mutableListOf<ManagerMember>()
        groupMembers.add(groupMember)
        val expired = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(IamGroupUtils.DEFAULT_EXPIRED_AT)
        val managerMemberGroup = ManagerMemberGroupDTO.builder().members(groupMembers).expiredAt(expired).build()
        iamV2ManagerService.createRoleGroupMemberV2(iamGroupId, managerMemberGroup)
    }
}
