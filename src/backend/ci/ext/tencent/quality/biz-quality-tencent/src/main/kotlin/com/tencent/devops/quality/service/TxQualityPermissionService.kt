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

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class TxQualityPermissionService @Autowired constructor(
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bkAuthResourceApi: AuthResourceApi,
    private val serviceCode: QualityAuthServiceCode,
    private val managerService: ManagerService,
    private val qualityRuleDao: QualityRuleDao,
    private val qualityGroupDao: QualityRuleDao,
    private val dslContext: DSLContext
) : QualityPermissionService {

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (managerService.isManagerPermission(
                userId = userId,
                projectId = projectId,
                authPermission = authPermission,
                resourceType = AuthResourceType.QUALITY_GROUP
            )) {
            return
        }

        if (!bkAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = serviceCode,
                resourceType = AuthResourceType.QUALITY_GROUP,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                permission = authPermission
            )) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw PermissionForbiddenException(
                message = message,
                params = arrayOf(permissionMsg))
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.createResource(
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.modifyResource(
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        bkAuthResourceApi.deleteResource(
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }

        val projectGroup = mutableListOf<Long>()
        qualityGroupDao.list(dslContext, projectId)?.map {
            projectGroup.add(it.id)
        }
        var isManager = false
        val managerPermissionMap = mutableMapOf<AuthPermission, List<Long>>()

        permissionResourceMap.keys.forEach {
            if (managerService.isManagerPermission(
                    userId = user,
                    projectId = projectId,
                    resourceType = AuthResourceType.QUALITY_GROUP,
                    authPermission = it
                )) {
                if (map[it] == null) {
                    managerPermissionMap[it] = projectGroup
                } else {
                    val collectionSet = mutableSetOf<Long>()
                    collectionSet.addAll(projectGroup.toSet())
                    collectionSet.addAll(map[it]!!.toSet())
                    managerPermissionMap[it] = collectionSet.toList()
                }

                isManager = true
            } else {
                managerPermissionMap[it] = map[it] ?: emptyList()
            }
        }

        if (isManager) {
            return managerPermissionMap
        }

        return map
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {

        val iamPermission = bkAuthPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            permission = authPermission
        )
        if (iamPermission) {
            return iamPermission
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectId,
            authPermission = authPermission,
            resourceType = AuthResourceType.QUALITY_RULE
        )
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = serviceCode,
                resourceType = AuthResourceType.QUALITY_RULE,
                projectCode = projectId,
                resourceCode = "*",
                permission = authPermission
            )) {

            if (!managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    authPermission = authPermission,
                    resourceType = AuthResourceType.QUALITY_RULE
                )) {
                val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                    defaultMessage = authPermission.alias
                )
                throw PermissionForbiddenException(
                    message = message,
                    params = arrayOf(permissionMsg)
                )
            }
        }
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        ruleId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = serviceCode,
                resourceType = AuthResourceType.QUALITY_RULE,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(ruleId),
                permission = authPermission
            )) {
            if (!managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    authPermission = authPermission,
                    resourceType = AuthResourceType.QUALITY_RULE
                )) {
                val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                    defaultMessage = authPermission.alias
                )
                throw PermissionForbiddenException(
                    message = message,
                    params = arrayOf(permissionMsg)
                )
            }
        }
    }

    override fun createRuleResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        bkAuthResourceApi.createResource(
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun modifyRuleResource(projectId: String, ruleId: Long, ruleName: String) {
        bkAuthResourceApi.modifyResource(
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        bkAuthResourceApi.deleteResource(
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId)
        )
    }

    override fun filterRules(
        userId: String,
        projectId: String,
        bkAuthPermissionSet: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            permissions = bkAuthPermissionSet,
            supplier = null
        )
        val permissionRuleMap = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { permission, list ->
            permissionRuleMap[permission] = list.map { HashUtil.decodeIdToLong(it) }
        }

        val projectRule = mutableListOf<Long>()
        qualityRuleDao.list(dslContext, projectId)?.map {
            projectRule.add(it.id)
        }
        var isManager = false
        val managerPermissionMap = mutableMapOf<AuthPermission, List<Long>>()

        permissionResourceMap.keys.forEach {
            if (managerService.isManagerPermission(
                    userId = userId,
                    projectId = projectId,
                    resourceType = AuthResourceType.QUALITY_RULE,
                    authPermission = it
                )) {
                if (permissionRuleMap[it] == null) {
                    managerPermissionMap[it] = projectRule
                } else {
                    val collectionSet = mutableSetOf<Long>()
                    collectionSet.addAll(projectRule.toSet())
                    collectionSet.addAll(permissionRuleMap[it]!!.toSet())
                    managerPermissionMap[it] = collectionSet.toList()
                }

                isManager = true
            } else {
                managerPermissionMap[it] = permissionRuleMap[it] ?: emptyList()
            }
        }

        if (isManager) {
            return managerPermissionMap
        }
        return permissionRuleMap
    }
}
