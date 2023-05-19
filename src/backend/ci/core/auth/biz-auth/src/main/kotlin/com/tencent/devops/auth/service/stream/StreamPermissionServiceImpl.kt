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

package com.tencent.devops.auth.service.stream

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.utils.ActionTypeUtils
import org.slf4j.LoggerFactory

abstract class StreamPermissionServiceImpl : PermissionService {
    override fun validateUserActionPermission(
        userId: String,
        action: String
    ): Boolean {
        // stream场景下不会使用到此场景. 做默认实现
        return false
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        // 如果有特殊权限,以特殊权限的校验结果为准
        val extPermission = extPermission(
            projectCode = projectCode,
            userId = userId,
            action = AuthPermission.get(action),
            resourceType = resourceType ?: ""
        )
        if (extPermission) {
            return extPermission
        }
        val projectType = isPublicProject(projectCode, userId)
        val projectMemberCheck = isProjectMember(projectCode, userId)
        val actionType = ActionTypeUtils.getActionType(action)
        logger.info("validete $userId|$projectCode|$action|$resourceType|$projectType|$projectMemberCheck")
        if (action == null) {
            return false
        }
        return actionType!!.permissionCheck(projectMemberCheck.first, projectType, projectMemberCheck.second)
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return validateUserResourcePermission(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceType = resourceType
        )
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = null
            )
        }
    }

    override fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resource.resourceCode,
                resourceType = resource.resourceType,
                relationResourceType = null
            )
        }
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        return validateUserResourcePermissionByRelation(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceType = resource.resourceType,
            resourceCode = resource.resourceCode,
            relationResourceType = null
        )
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        // 校验项目权限， 有就返回全部
        if (validateUserResourcePermission(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = resourceType
        )) {
            return arrayListOf("*")
        }
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val instanceMap = mutableMapOf<AuthPermission, List<String>>()
        actions.forEach {
            val actionList = getUserResourceByAction(userId, it, projectCode, resourceType)
            instanceMap[AuthPermission.get(it)] = actionList
        }
        return instanceMap
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        return actions.associate { action ->
            val authPermission = action.substringAfterLast("_")
            AuthPermission.get(authPermission) to resources.map { it.resourceCode }
        }
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        return emptyMap()
    }

    /**
     * 是否是开源项目
     * projectCode: stream侧项目编码
     */
    abstract fun isPublicProject(projectCode: String, userId: String?): Boolean

    /**
     * 是否是项目成员
     * projectCode: stream侧项目编码
     * userId: 待校验用户
     */
    abstract fun isProjectMember(
        projectCode: String,
        userId: String
    ): Pair<Boolean/**是否为项目成员*/, Boolean/**是否为developer以上权限*/>

    /**
     * 扩展权限(如本地管理员之类的特殊逻辑)
     *  projectCode: stream侧项目编码
     * userId: 待校验用户
     * action: stream内定义的操作类型
     */
    abstract fun extPermission(
        projectCode: String,
        userId: String,
        action: AuthPermission,
        resourceType: String
    ): Boolean

    companion object {
        val logger = LoggerFactory.getLogger(StreamPermissionServiceImpl::class.java)
    }
}
