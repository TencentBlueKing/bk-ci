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
 */

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateSubsetManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateSubsetManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dispatcher.AuthResourceGroupDispatcher
import com.tencent.devops.auth.pojo.event.AuthResourceGroupEvent
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PermissionSubsetManagerService @Autowired constructor(
    private val permissionScopesService: PermissionScopesService,
    private val iamV2ManagerService: V2ManagerService,
    private val dslContext: DSLContext,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceGroupDispatcher: AuthResourceGroupDispatcher
) {

    /**
     * 创建二级管理员
     */
    @SuppressWarnings("LongParameterList")
    fun createSubsetManager(
        gradeManagerId: String,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val managerGroupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.DEFAULT_GROUP_NOT_FOUND,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "权限系统：资源类型${resourceType}关联的默认组${DefaultGroupType.MAINTAINER.value}不存在"
        )
        val name = IamGroupUtils.buildSubsetManagerGroupName(
            resourceName = resourceName,
            groupName = managerGroupConfig.groupName
        )
        val description = IamGroupUtils.buildSubsetManagerDescription(
            resourceName = resourceName,
            userId = userId
        )
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
        val createSubsetManagerDTO = CreateSubsetManagerDTO.builder()
            .name(name)
            .description(description)
            .members(listOf(userId))
            .authorizationScopes(authorizationScopes)
            .inheritSubjectScope(true)
            .subjectScopes(listOf())
            .syncPerm(true)
            .build()
        val subsetManagerId = iamV2ManagerService.createSubsetManager(
            gradeManagerId,
            createSubsetManagerDTO
        )
        authResourceGroupDispatcher.dispatch(
            AuthResourceGroupEvent(
                managerId = subsetManagerId,
                userId = userId,
                projectCode = projectCode,
                projectName = projectName,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName
            )
        )
        return subsetManagerId
    }

    @SuppressWarnings("LongParameterList")
    fun modifySubsetManager(
        subsetManagerId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        val managerGroupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.DEFAULT_GROUP_NOT_FOUND,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "权限系统：资源类型${resourceType}关联的默认组${DefaultGroupType.MAINTAINER.value}不存在"
        )
        val name = IamGroupUtils.buildSubsetManagerGroupName(
            resourceName = resourceName,
            groupName = managerGroupConfig.groupName
        )
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
        val subsetManagerDetail = iamV2ManagerService.getSubsetManagerDetail(subsetManagerId)
        val updateSubsetManagerDTO = UpdateSubsetManagerDTO.builder()
            .name(name)
            .members(subsetManagerDetail.members)
            .description(subsetManagerDetail.description)
            .authorizationScopes(authorizationScopes)
            .inheritSubjectScope(true)
            .subjectScopes(listOf())
            .syncPerm(true)
            .build()
        iamV2ManagerService.updateSubsetManager(
            subsetManagerId,
            updateSubsetManagerDTO
        )
        return true
    }

    fun deleteSubsetManager(subsetManagerId: String) {
        iamV2ManagerService.deleteSubsetManager(subsetManagerId)
    }

    fun listGroup(
        subsetManagerId: String
    ): List<IamGroupInfoVo> {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 10
        val iamGroupInfoList =
            iamV2ManagerService.getSubsetManagerRoleGroup(subsetManagerId.toInt(), pageInfoDTO)
        return iamGroupInfoList.results.map {
            IamGroupInfoVo(
                id = it.id,
                name = it.name,
                displayName = IamGroupUtils.getGroupDisplayName(it.name),
                userCount = it.userCount,
                departmentCount = it.departmentCount
            )
        }.sortedBy { it.id }
    }
}
