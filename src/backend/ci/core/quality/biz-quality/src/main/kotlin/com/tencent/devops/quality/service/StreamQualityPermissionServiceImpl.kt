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

package com.tencent.devops.quality.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class StreamQualityPermissionServiceImpl @Autowired constructor(
    private val client: Client,
    private val tokenCheckService: ClientTokenService,
    private val ruleDao: QualityRuleDao,
    private val groupDao: QualityNotifyGroupDao,
    private val dslContext: DSLContext
) : QualityPermissionService {
    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = validateGroupPermission(
            userId = userId,
            projectId = projectId,
            authPermission = authPermission
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        if (authPermission == AuthPermission.LIST || authPermission == AuthPermission.CREATE)
            return true
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = authPermission.value,
            projectCode = projectId,
            resourceCode = AuthResourceType.QUALITY_GROUP_NEW.value
        ).data ?: false
    }

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = validateGroupPermission(
            userId = userId,
            projectId = projectId,
            authPermission = authPermission
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(
        userId: String,
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        return
    }

    override fun modifyGroupResource(
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        return
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        return
    }

    override fun filterGroup(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        // 此处不同的AuthPermission类型有不同的校验逻辑。需要拆分
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        val groupInfos = groupDao.list(dslContext, projectId, 0, 1000).map { it.id }

        authPermissions.forEach {
            val permissionCheck = validateGroupPermission(
                userId = userId,
                projectId = projectId,
                authPermission = it
            )
            if (!permissionCheck) {
                resultMap[it] = emptyList()
            } else {
                resultMap[it] = groupInfos
            }
        }
        return resultMap
    }

    override fun filterListPermissionGroups(
        userId: String,
        projectId: String,
        allGroupIds: List<Long>
    ): List<Long> = allGroupIds

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        if (authPermission == AuthPermission.LIST)
            return true
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = authPermission.value,
            projectCode = projectId,
            resourceCode = AuthResourceType.QUALITY_RULE.value
        ).data ?: false
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validateRulePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        ruleId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validateRulePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createRuleResource(
        userId: String,
        projectId: String,
        ruleId: Long,
        ruleName: String
    ) {
        return
    }

    override fun modifyRuleResource(
        projectId: String,
        ruleId: Long,
        ruleName: String
    ) {
        return
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        return
    }

    override fun filterRules(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        // 此处不同的AuthPermission类型有不同的校验逻辑。需要拆分
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        val ruleInfos = ruleDao.list(dslContext, projectId)?.map { it.id } ?: emptyList()

        authPermissions.forEach {
            val permissionCheck = validateRulePermission(
                userId = userId,
                projectId = projectId,
                authPermission = it
            )
            if (!permissionCheck) {
                resultMap[it] = emptyList()
            } else {
                resultMap[it] = ruleInfos
            }
        }
        return resultMap
    }

    override fun filterListPermissionRules(
        userId: String,
        projectId: String,
        allRulesIds: List<Long>
    ): List<Long> = allRulesIds
}
