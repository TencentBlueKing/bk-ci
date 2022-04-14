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

package com.tencent.devops.auth.service.simple

import com.tencent.devops.auth.service.AuthCustomizePermissionService
import com.tencent.devops.auth.service.AuthGroupMemberService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.StrategyService
import com.tencent.devops.auth.service.action.ActionService
import com.tencent.devops.auth.service.ci.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class SimpleAuthPermissionService @Autowired constructor(
    val groupService: AuthGroupService,
    val actionService: ActionService,
    val groupMemberService: AuthGroupMemberService,
    val authCustomizePermissionService: AuthCustomizePermissionService,
    val strategyService: StrategyService
) : PermissionService {
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        if (isAdmin(userId)) {
            return true
        }
        actionService.checkSystemAction(arrayListOf(action))
        /**
         * 1. 查询用户在项目下加入了哪些组
         * 2. 默认用户组校验
         * 3. 自定义用户组校验
         */
        // 获取用户加入的用户组
        val groupInfos = groupMemberService.getUserGroupByProject(userId, projectCode) ?: return false
        val defaultGroup = mutableListOf<Int>()
        val customizeGroup = mutableListOf<Int>()
        groupInfos.forEach {
            if (it.groupType) {
                defaultGroup.add(it.groupId.toInt())
            } else {
                customizeGroup.add(it.groupId.toInt())
            }
        }

        // 默认用户组权限校验
        if (defaultGroup.isNotEmpty()) {
            defaultGroup.forEach {
                val groupCode = groupService.getGroupCode(it)?.groupCode ?: return@forEach
                val checkDefaultPermission = strategyService.checkDefaultStrategy(groupCode, resourceType!!, action)
                if (checkDefaultPermission) {
                    return true
                }
            }
        }
        // 自定义用组权限权限校验
        if (customizeGroup.isNotEmpty()) {
            customizeGroup.forEach {
                val checkPermission = authCustomizePermissionService.checkCustomizePermission(
                    groupId = it,
                    action = action,
                    resourceType = resourceType!!
                )
                if (checkPermission) {
                    return true
                }
            }
        }

        return false
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return validateUserResourcePermission(userId, action, projectCode, resourceType)
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        if (isAdmin(userId)) {
            return arrayListOf("*")
        }
        logger.info("getUserResourceByAction $userId $action $projectCode $resourceType")
        // 暂时未做实例级别权限控制。 有操作权限，就有所有的实例权限
        return if (validateUserResourcePermission(userId, action, projectCode, resourceType)) {
            arrayListOf("*")
        } else {
            emptyList()
        }
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val instances = mutableMapOf<AuthPermission, List<String>>()
        actions.forEach {
            instances[AuthPermission.get(it)] = getUserResourceByAction(userId, it, projectCode, resourceType)
        }
        return instances
    }

    /**
     * admin为系统管理员
     */
    private fun isAdmin(userId: String): Boolean {
        if (userId == "admin") {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(SimpleAuthPermissionService::class.java)
    }
}
