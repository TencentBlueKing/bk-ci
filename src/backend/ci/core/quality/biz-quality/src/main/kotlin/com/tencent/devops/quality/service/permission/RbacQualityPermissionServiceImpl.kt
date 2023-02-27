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

package com.tencent.devops.quality.service.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.quality.service.QualityPermissionService
import org.jooq.DSLContext

class RbacQualityPermissionServiceImpl(
    val client: Client,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService
) : QualityPermissionService {
    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.QUALITY_GROUP),
            relationResourceType = null,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.QUALITY_GROUP)
        ).data ?: false
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_GROUP_NEW.value,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_GROUP_NEW.value,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_GROUP_NEW.value,
            resourceCode = HashUtil.encodeLongId(groupId),
        )
    }

    override fun filterGroup(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val actions = mutableListOf<String>()
        authPermissions.forEach {
            actions.add(RbacAuthUtils.buildAction(it, AuthResourceType.QUALITY_GROUP))
        }
        val instancesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.QUALITY_GROUP),
            action = actions
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instancesMap)
    }

    override fun validateRulePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.QUALITY_RULE),
            resourceCode = RbacAuthUtils.extResourceType(AuthResourceType.QUALITY_RULE)
        ).data ?: false
    }

    override fun validateRulePermission(userId: String, projectId: String, authPermission: AuthPermission, message: String) {
        if (!validateRulePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateRulePermission(userId: String, projectId: String, ruleId: Long, authPermission: AuthPermission, message: String) {
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.QUALITY_RULE),
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.QUALITY_RULE),
            relationResourceType = null
        ).data ?: false
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createRuleResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_RULE.value,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun modifyRuleResource(projectId: String, ruleId: Long, ruleName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_RULE.value,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = AuthResourceType.QUALITY_RULE.value,
            resourceCode = HashUtil.encodeLongId(ruleId),
        )
    }

    override fun filterRules(userId: String, projectId: String, bkAuthPermissionSet: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val actions = mutableListOf<String>()
        bkAuthPermissionSet.forEach {
            actions.add(RbacAuthUtils.buildAction(it, AuthResourceType.QUALITY_RULE))
        }
        val instancesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.QUALITY_RULE),
            action = actions
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instancesMap)
    }
}
