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
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@SuppressWarnings("LongParameterList")
class AuthResourceGroupService(
    private val dslContext: DSLContext,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val iamV2ManagerService: V2ManagerService,
    private val permissionScopesService: PermissionScopesService,
    private val authResourceGroupDao: AuthResourceGroupDao
) {

    fun createGradeDefaultGroup(
        gradeManagerId: Int,
        userId: String,
        projectCode: String,
        projectName: String
    ) {
        syncGradeManagerGroup(gradeManagerId = gradeManagerId, projectCode = projectCode)
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            createMode = false
        )
        defaultGroupConfigs.filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }.forEach { groupConfig ->
            val name = groupConfig.groupName
            val description = IamGroupUtils.buildDefaultDescription(
                projectName = projectName,
                groupName = name,
                userId = userId
            )
            val managerRoleGroup = ManagerRoleGroup(name, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            val iamGroupId = iamV2ManagerService.batchCreateRoleGroupV2(gradeManagerId, managerRoleGroupDTO)
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = groupConfig.groupCode,
                groupName = name,
                relationId = iamGroupId.toString()
            )
            grantGradeManagerGroupPermission(
                projectCode = projectCode,
                projectName = projectName,
                groupCode = groupConfig.groupCode,
                iamGroupId = iamGroupId
            )
            addGroupMember(userId = userId, iamGroupId = iamGroupId)
        }
    }

    /**
     * 同步创建分级管理员时自动创建的组
     */
    fun syncGradeManagerGroup(
        gradeManagerId: Int,
        projectCode: String
    ) {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId.toString(),
            null,
            pageInfoDTO
        )
        iamGroupInfoList.results.map { iamGroupInfo ->
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = iamGroupInfo.name,
                relationId = iamGroupInfo.id.toString()
            )
        }
    }

    /**
     * 创建二级管理员默认分组
     *
     * @param createMode false-创建资源时就创建默认分组,true-启用资源时才创建
     */
    fun createSubsetManagerDefaultGroup(
        subsetManagerId: Int,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        createMode: Boolean
    ) {
        syncSubsetManagerGroup(
            subsetManagerId = subsetManagerId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val resourceGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            createMode = createMode
        )
        resourceGroupConfigs.filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }.forEach { groupConfig ->
            val name = IamGroupUtils.buildSubsetManagerGroupName(
                resourceName = resourceName,
                groupName = groupConfig.groupName
            )
            val description = IamGroupUtils.buildSubsetManagerGroupDescription(
                resourceName = resourceName,
                groupName = name,
                userId = userId
            )
            val managerRoleGroup = ManagerRoleGroup(name, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            val iamGroupId = iamV2ManagerService.batchCreateSubsetRoleGroup(subsetManagerId, managerRoleGroupDTO)
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = groupConfig.groupCode,
                groupName = name,
                relationId = iamGroupId.toString()
            )
            grantSubsetManagerGroupPermission(
                projectCode = projectCode,
                projectName = projectName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                groupCode = groupConfig.groupCode,
                iamGroupId = iamGroupId
            )
            addGroupMember(userId = userId, iamGroupId = iamGroupId)
        }
    }

    /**
     * 同步二级管理员创建时自动创建的用户组
     */
    fun syncSubsetManagerGroup(
        subsetManagerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val iamGroupInfoList =
            iamV2ManagerService.getSubsetManagerRoleGroup(subsetManagerId, pageInfoDTO)
        iamGroupInfoList.results.map { iamGroupInfo ->
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = iamGroupInfo.name,
                relationId = iamGroupInfo.id.toString()
            )
        }
    }

    fun grantSubsetManagerGroupPermission(
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        groupCode: String,
        iamGroupId: Int
    ) {
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = groupCode
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

    fun grantGradeManagerGroupPermission(
        projectCode: String,
        projectName: String,
        groupCode: String,
        iamGroupId: Int
    ) {
        val authorizationScopes = permissionScopesService.buildGradeManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = groupCode
            ),
            projectCode = projectCode,
            projectName = projectName
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
