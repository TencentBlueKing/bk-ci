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

package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

interface PermissionResourceGroupPermissionService {
    fun grantGroupPermission(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        /*用户组所属的资源类型*/
        resourceType: String,
        groupCode: String,
        iamResourceCode: String,
        resourceName: String,
        iamGroupId: Int,
        registerMonitorPermission: Boolean = true,
        // 若filterResourceTypes不为空，则本次新增的组权限，只和该资源类型有关
        filterResourceTypes: List<String> = emptyList(),
        // 若filterActions不为空，则本次新增的组权限，只和该操作有关
        filterActions: List<String> = emptyList()
    ): Boolean

    /**
     *  授予项目级用户组权限，例子：给项目级用户组都添加上流水线列表权限。
     * */
    fun grantAllProjectGroupsPermission(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ): Boolean

    /**
     *  构建项目级权限，如传递actions为pipeline_view，将获得整个项目下流水线的查询权限。
     * */
    fun buildProjectPermissions(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ): String

    fun getGroupPolices(
        userId: String,
        projectCode: String,
        resourceType: String,
        iamGroupId: Int
    ): List<IamGroupPoliciesVo>

    fun deleteByGroupIds(
        projectCode: String,
        iamGroupIds: List<Int>
    ): Boolean

    fun listGroupsByPermissionConditions(
        projectCode: String,
        filterIamGroupIds: List<Int>? = null,
        relatedResourceType: String,
        relatedResourceCode: String? = null,
        action: String? = null
    ): List<Int>

    /**
     *   校验项目级权限，参数 relatedResourceType:project、relatedResourceCode:projectCode
     *   校验具体资源级权限，如校验是否有某条流水线权限，参数  relatedResourceType:pipeline、relatedResourceCode:p-1
     * */
    fun isGroupsHasPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        relatedResourceCode: String,
        action: String
    ): Boolean

    /**
     * 是否用户拥有项目级别权限，如整个项目流水线执行权限/项目的管理权限等。
     * */
    fun isGroupsHasProjectLevelPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        action: String
    ): Boolean

    /**
     * 获取用户组有权限的资源--按照资源类型区分
     * */
    fun listGroupResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): Map<String/*resourceType*/, List<String>/*resourceCodes*/>

    /**
     * 获取用户组有权限的资源
     * */
    fun listResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): List<String>

    fun getGroupPermissionDetail(
        iamGroupId: Int
    ): Map<String, List<GroupPermissionDetailVo>>

    fun getGroupPermissionDetailBySystem(
        iamSystemId: String,
        iamGroupId: Int
    ): List<GroupPermissionDetailVo>

    fun syncGroupPermissions(
        projectCode: String,
        iamGroupId: Int
    ): Boolean

    fun syncProjectPermissions(
        projectCode: String
    ): Boolean

    fun syncPermissionsByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Boolean

    fun deleteByResource(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    fun syncProjectLevelPermissions(
        projectCode: String
    ): Boolean

    fun syncProjectLevelPermissions(
        projectCode: String,
        iamGroupId: Int
    ): Boolean

    fun syncProjectLevelPermissionsByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Boolean

    fun listProjectsWithPermission(
        memberIds: List<String>,
        action: String
    ): List<String>
}
