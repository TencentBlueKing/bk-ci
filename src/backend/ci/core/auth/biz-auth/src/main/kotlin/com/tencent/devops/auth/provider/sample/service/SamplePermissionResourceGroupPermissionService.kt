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

package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

class SamplePermissionResourceGroupPermissionService : PermissionResourceGroupPermissionService {
    override fun grantGroupPermission(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        groupCode: String,
        iamResourceCode: String,
        resourceName: String,
        iamGroupId: Int,
        registerMonitorPermission: Boolean,
        filterResourceTypes: List<String>,
        filterActions: List<String>
    ): Boolean = true

    override fun grantAllProjectGroupsPermission(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ) = true

    override fun buildProjectPermissions(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ) = ""

    override fun getGroupPolices(
        userId: String,
        projectCode: String,
        resourceType: String,
        iamGroupId: Int
    ): List<IamGroupPoliciesVo> = emptyList()

    override fun deleteByGroupIds(
        projectCode: String,
        iamGroupIds: List<Int>
    ): Boolean = true

    override fun listGroupsByPermissionConditions(
        projectCode: String,
        filterIamGroupIds: List<Int>?,
        relatedResourceType: String,
        relatedResourceCode: String?,
        action: String?
    ): List<Int> = emptyList()

    override fun isGroupsHasPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        relatedResourceCode: String,
        action: String
    ): Boolean = true

    override fun isGroupsHasProjectLevelPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        action: String
    ): Boolean = true

    override fun listGroupResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): Map<String, List<String>> = emptyMap()

    override fun listResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): List<String> = emptyList()

    override fun getGroupPermissionDetail(
        iamGroupId: Int
    ): Map<String, List<GroupPermissionDetailVo>> = emptyMap()

    override fun getGroupPermissionDetailBySystem(
        iamSystemId: String,
        iamGroupId: Int
    ): List<GroupPermissionDetailVo> = emptyList()

    override fun syncGroupPermissions(
        projectCode: String,
        iamGroupId: Int
    ): Boolean = true

    override fun syncProjectPermissions(
        projectCode: String
    ): Boolean = true

    override fun syncPermissionsByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Boolean = true

    override fun deleteByResource(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean = true

    override fun syncProjectLevelPermissions(
        projectCode: String,
        iamGroupId: Int
    ): Boolean = true

    override fun syncProjectLevelPermissions(
        projectCode: String
    ): Boolean = true

    override fun syncProjectLevelPermissionsByCondition(projectConditionDTO: ProjectConditionDTO): Boolean = true

    override fun listProjectsWithPermission(
        memberIds: List<String>,
        action: String
    ): List<String> = emptyList()
}
