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

package com.tencent.devops.common.auth.api

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.utils.ActionUtils
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class BluekingV3AuthPermissionApi @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val redisOperation: RedisOperation,
    private val iamConfiguration: IamConfiguration
) : AuthPermissionApi {
    override fun addResourcePermissionForUsers(
        userId: String,
        projectCode: String,
        serviceCode: AuthServiceCode,
        permission: AuthPermission,
        resourceType: AuthResourceType,
        resourceCode: String,
        userIdList: List<String>,
        supplier: (() -> List<String>)?
    ): Boolean {
        return true
    }

    // 判断用户是否有某个动作的权限。 该动作无需绑定实例。如：判断是否有创建权限，创建无需挂任何实例。 若要判断是否有某实例的权限不能用该接口。
    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        val actionType = ActionUtils.buildAction(resourceType, permission)
        return authHelper.isAllowed(user, actionType)
    }

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission,
        resource: AuthResourceInstance
    ): Boolean {
        return validateUserResourcePermission(
            user = user,
            serviceCode = serviceCode,
            resourceType = AuthResourceType.get(resource.resourceType),
            projectCode = projectCode,
            resourceCode = resource.resourceCode,
            permission = permission
        )
    }

    // 判断用户是否有某个动作某个实例的权限。
    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: AuthPermission,
        relationResourceType: AuthResourceType?
    ): Boolean {
        if (isProjectOwner(projectCode, user)) {
            return true
        }

        val actionType = ActionUtils.buildAction(resourceType, permission)
        val instanceDTO = InstanceDTO()
        instanceDTO.system = serviceCode.id()
        // 若不关注操作资源实例，则必须关注是否在项目下
        if (resourceCode == "*") {
            instanceDTO.id = projectCode
            instanceDTO.type = AuthResourceType.PROJECT.value
        } else {
            instanceDTO.id = resourceCode
            instanceDTO.type = resourceType.value

            // 因除项目外的所有资源都需关联项目, 需要拼接策略path供sdk计算
            val path = PathInfoDTO()
            path.type = AuthResourceType.PROJECT.value
            path.id = projectCode
            instanceDTO.path = path
        }
        // 有可能出现提供的resourceCode是关联项目资源的code,需将type类型调整为对应的关联资源。
        if (relationResourceType != null) {
            instanceDTO.type = relationResourceType!!.value
        }

        logger.info("v3 validateUserResourcePermission instanceDTO[$instanceDTO]")
        return authHelper.isAllowed(user, actionType, instanceDTO)
    }

    // 获取用户某动作下的所有有权限的实例。 如 获取A项目下的所有有查看权限的流水线
    override fun getUserResourceByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        val actionType = ActionUtils.buildAction(resourceType, permission)
        val actionDto = ActionDTO()
        actionDto.id = actionType
        val expression = (policyService.getPolicyByAction(user, actionDto, null) ?: return emptyList())
        logger.info("getUserResourceByPermission expression:$expression")

        if (expression.operator == null && expression.content == null) {
            return emptyList()
        }

        // 管理员权限
        if (expression.operator == ExpressionOperationEnum.ANY) {
            return listOf("*")
        }

        return if (resourceType == AuthResourceType.PROJECT) {
            AuthUtils.getProjects(expression)
        } else {
            val instancesList = AuthUtils.getResourceInstance(expression, projectCode, resourceType.value)
            if (!instancesList.contains("*")) {
                instancesList.toList()
            } else {
                listOf("*")
            }
        }
    }

    override fun getUserResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode, // 对应新版的systemId
        resourceType: AuthResourceType,
        projectCode: String,
        permissions: Set<AuthPermission>,
        supplier: (() -> List<String>)?
    ): Map<AuthPermission, List<String>> {
        return getUserResourcesByPermissions(
            userId = user,
            scopeType = "project",
            scopeId = projectCode,
            resourceType = resourceType,
            permissions = permissions,
            systemId = serviceCode,
            supplier = supplier
        )
    }

    override fun getUserResourcesByPermissions(
        userId: String,
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        permissions: Set<AuthPermission>,
        systemId: AuthServiceCode,
        supplier: (() -> List<String>)?
    ): Map<AuthPermission, List<String>> {
        val permissionMap = mutableMapOf<AuthPermission, List<String>>()
        permissions.map {
            val instances = getUserResourceByPermission(
                user = userId,
                serviceCode = systemId,
                resourceType = resourceType,
                permission = it,
                projectCode = scopeId,
                supplier = supplier
            )
            permissionMap[it] = instances
        }
        logger.info("v3 getPermissionMap user[$userId], project[$scopeId] map[$permissionMap]")
        return permissionMap
    }

    override fun getUserResourceAndParentByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission,
        resourceType: AuthResourceType
    ): Map<String, List<String>> {
        return emptyMap()
    }

    override fun filterResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permissions: Set<AuthPermission>,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    // 此处为不在common内依赖业务接口，固只从redis内取，前置有写入逻辑
    // 若前置失效会导致log,dispatch, artifactory等校验权限出现： 项目管理员没有该项目下其他人创建的某资源的权限。 处理概率极小
    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val projectOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (!projectOwner.isNullOrEmpty()) {
            return projectOwner == userId
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(BluekingV3AuthPermissionApi::class.java)
    }
}
