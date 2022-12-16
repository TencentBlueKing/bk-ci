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

import com.tencent.bk.sdk.iam.dto.manager.dto.CreateSubsetManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.enums.RbacDefaultGroupType
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.auth.service.iam.PermissionSubsetManagerService
import com.tencent.devops.common.auth.utils.IamGroupUtils

class RbacPermissionSubsetManagerService(
    private val permissionScopesService: PermissionScopesService,
    private val iamV2ManagerService: V2ManagerService
) : PermissionSubsetManagerService {

    override fun createSubsetManager(
        gradeManagerId: String,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val name = IamGroupUtils.buildSubsetManagerGroupName(
            resourceName = resourceName,
            groupDisplayName = RbacDefaultGroupType.OWNER.displayName
        )
        val description = IamGroupUtils.buildSubsetManagerDescription(
            resourceName = resourceName,
            userId = userId
        )
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = RbacDefaultGroupType.OWNER.getStrategyName(resourceType = resourceType),
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
            .authorizationScopes(listOf(authorizationScopes))
            .inheritSubjectScope(false)
            .build()
        return iamV2ManagerService.createSubsetManager(
            gradeManagerId,
            createSubsetManagerDTO
        )
    }
}
