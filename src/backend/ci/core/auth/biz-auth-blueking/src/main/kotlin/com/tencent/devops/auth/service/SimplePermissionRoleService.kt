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

import com.tencent.devops.auth.pojo.DefaultGroup
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.pojo.vo.GroupInfoVo
import com.tencent.devops.auth.service.action.ActionService
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionRoleServiceImpl
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

/**
 * 蓝盾开源内置权限实现
 */
class SimplePermissionRoleService @Autowired constructor(
    private val dslContext: DSLContext,
    private val groupService: AuthGroupService,
    private val resourceService: BkResourceService,
    private val actionsService: ActionService,
    private val authCustomizePermissionService: AuthCustomizePermissionService
): AbsPermissionRoleServiceImpl(groupService, resourceService, actionsService) {

    override fun groupCreateExt(
        roleId: Int,
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ) {
        // 默认用户组权限模版统一存在Strategy表内，无需建立用户组与权限的映射数据
        // 用户自定义用户组需将数据存入Customize表内建立用户组与权限的映射关系
        if (groupInfo.defaultGroup == true) {
            return
        }
        groupInfo.actionMap.forEach { resource, actions ->
            authCustomizePermissionService.createCustomizePermission(
                userId = userId,
                groupId = roleId,
                resourceType = resource,
                actions = actions
            )
        }

    }

    override fun updateGroupExt(
        userId: String,
        projectId: Int,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    ) {
        TODO("Not yet implemented")
    }

    override fun deleteRoleExt(
        userId: String,
        projectId: Int,
        roleId: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun rolePermissionStrategyExt(
        userId: String,
        projectCode: String,
        roleId: Int,
        permissionStrategy: Map<String, List<String>>
    ): Boolean {
        permissionStrategy.forEach { resource, actions ->
            authCustomizePermissionService.createCustomizePermission(
                userId = userId,
                groupId = roleId,
                resourceType = resource,
                actions = actions
            )
        }
        return true
    }

    override fun getPermissionRole(projectId: Int): List<GroupInfoVo> {
        TODO("Not yet implemented")
    }

    override fun getDefaultRole(): List<DefaultGroup> {
        val groups = mutableListOf<DefaultGroup>()
        val defaultGroup = DefaultGroupType.getAll()
        defaultGroup.forEach {
            DefaultGroup(
                code = it.name,
                name = it.name,
                displayName = it.displayName
            )
        }
        return groups
    }
}