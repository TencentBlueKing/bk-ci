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

package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.ci.PermissionService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

open class IamPermissionServiceImpl @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration,
    private val iamCacheService: IamCacheService
): PermissionService {

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        logger.info("[iam V3] validateUserActionPermission $userId $action")
        return authHelper.isAllowed(userId, action)
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        return validateUserResourcePermissionByRelation(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            relationResourceType = null
        )
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        logger.info("[iam V3]validateUserResourcePermissionByRelation: $userId $action $projectCode " +
                        "$resourceCode $resourceType $relationResourceType")

        if (iamCacheService.checkProjectManager(userId, projectCode)) {
            return true
        }

        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        // 若不关注操作资源实例，则必须关注是否在项目下
        if (resourceCode == "*") {
            instanceDTO.id = projectCode
            instanceDTO.type = AuthResourceType.PROJECT.value
        } else {
            instanceDTO.id = resourceCode
            instanceDTO.type = resourceType

            // 因除项目外的所有资源都需关联项目, 需要拼接策略path供sdk计算
            val path = PathInfoDTO()
            path.type = AuthResourceType.PROJECT.value
            path.id = projectCode
            instanceDTO.path = path
        }
        // 有可能出现提供的resourceCode是关联项目资源的code,需将type类型调整为对应的关联资源。
        if (relationResourceType != null) {
            instanceDTO.type = relationResourceType
        }

        logger.info("[iam V3] validateUserResourcePermission instanceDTO[$instanceDTO]")
        return authHelper.isAllowed(userId, action, instanceDTO)
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        try {
            logger.info("[iam V3] getUserResourceByPermission $userId $action $projectCode $resourceType")
            // 管理员直接返回“*”
            if (iamCacheService.checkProjectManager(userId, projectCode)) {
                return arrayListOf("*")
            }
            val expression = iamCacheService.getUserExpression(userId, action, resourceType)
            logger.info("[iam V3] getUserResourceByPermission action: $action, expression:$expression")

            if (expression == null) {
                return emptyList()
            }

            if (expression.operator == null && expression.content == null) {
                return emptyList()
            }

            // 管理员权限
            if (expression.operator == ExpressionOperationEnum.ANY) {
                return listOf("*")
            }

            return if (resourceType == AuthResourceType.PROJECT.value) {
                AuthUtils.getProjects(expression)
            } else {
                val instancesList = AuthUtils.getResourceInstance(expression, projectCode, resourceType)
                if (!instancesList.contains("*")) {
                    instancesList.toList()
                } else {
                    listOf("*")
                }
            }
        } catch (e: Exception) {
            logger.warn("getUserResourceByAction fail {}", e)
        }
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        logger.info("[iam V3] getUserResourcesByActions $userId $actions $projectCode $resourceType")
        val result = mutableMapOf<AuthPermission, List<String>>()
        actions.forEach {
            val actionResourceList = getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
            val authPermission = it.substringAfterLast("_")
            result[AuthPermission.get(authPermission)] = actionResourceList
        }
        return result
    }

    companion object {
        val logger = LoggerFactory.getLogger(IamPermissionServiceImpl::class.java)
    }
}