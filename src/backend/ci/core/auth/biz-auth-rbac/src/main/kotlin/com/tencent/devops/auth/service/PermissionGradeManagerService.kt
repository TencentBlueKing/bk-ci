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

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PermissionGradeManagerService @Autowired constructor(
    private val permissionScopesService: PermissionScopesService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val permissionResourceGroupService: PermissionResourceGroupService
) {

    /**
     * 创建分级管理员
     */
    fun createGradeManager(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = resourceName,
        )
        val description = IamGroupUtils.buildManagerDescription(
            projectName = resourceName,
            userId = userId
        )
        val authorizationScopes = permissionScopesService.buildGradeManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName
        )
        val createManagerDTO = CreateManagerDTO.builder()
            .system(iamConfiguration.systemId)
            .name(name)
            .description(description)
            .members(listOf(userId))
            .authorization_scopes(authorizationScopes)
            .subject_scopes(listOf(ManagerScopes("*", "*")))
            .sync_perm(true)
            .build()
        val gradeManagerId = iamV2ManagerService.createManagerV2(createManagerDTO)
        permissionResourceGroupService.createGradeDefaultGroup(
            gradeManagerId = gradeManagerId,
            userId = userId,
            projectCode = projectCode,
            projectName = projectName
        )
        return gradeManagerId
    }

    fun listGroup(
        gradeManagerId: String
    ): List<IamGroupInfoVo> {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 10
        val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId,
            null,
            pageInfoDTO
        )
        return iamGroupInfoList.results.map {
            IamGroupInfoVo(
                id = it.id,
                name = it.name,
                displayName = IamGroupUtils.getSubsetManagerGroupDisplayName(it.name),
                userCount = it.userCount,
                departmentCount = it.departmentCount
            )
        }.sortedBy { it.id }
    }
}
