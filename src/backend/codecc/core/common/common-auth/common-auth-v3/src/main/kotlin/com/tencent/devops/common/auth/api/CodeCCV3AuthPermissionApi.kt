package com.tencent.devops.common.auth.api

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.utils.AuthStrUtils
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired


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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

class CodeCCV3AuthPermissionApi @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val redisOperation: RedisOperation
): AuthPermissionStrApi {
    override fun addResourcePermissionForUsers(userId: String,
                                               projectCode: String,
                                               serviceCode: AuthServiceCode,
                                               permission: String,
                                               resourceType: String,
                                               resourceCode: String,
                                               userIdList: List<String>,
                                               supplier: (() -> List<String>)?): Boolean {
        return true
    }

    override fun getUserResourceByPermission(user: String,
                                             serviceCode: AuthServiceCode,
                                             resourceType: String,
                                             projectCode: String,
                                             permission: String,
                                             supplier: (() -> List<String>)?): List<String> {
        logger.info("v3 getUserResourceByPermission user[$user] serviceCode[${serviceCode.id()}] resourceType[${resourceType}] projectCode[$projectCode] permission[${permission}] supplier[$supplier]")
        val actionType = buildAction(resourceType, permission)
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

        return if (resourceType == AuthResourceType.PROJECT.name) {
            AuthStrUtils.getProjects(expression)
        } else {
            val instancesList = AuthStrUtils.getResourceInstance(expression, projectCode, resourceType)
            logger.info("getUserResourceByPermission getInstance project[$projectCode], type[${resourceType}], instances[$instancesList]")
            if (!instancesList.contains("*")) {
                instancesList.toList()
            } else {
                listOf("*")
            }
        }
    }

    override fun getUserResourcesByPermissions(user: String, serviceCode: AuthServiceCode, resourceType: String, projectCode: String, permissions: Set<String>, supplier: (() -> List<String>)?): Map<String, List<String>> {
        logger.info("v3 getUserResourcesByPermissions user[$user] serviceCode[$serviceCode] resourceType[$resourceType] projectCode[$projectCode] permission[$permissions] supplier[$supplier]")
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

    override fun getUserResourcesByPermissions(userId: String, scopeType: String, scopeId: String, resourceType: String, permissions: Set<String>, systemId: AuthServiceCode, supplier: (() -> List<String>)?): Map<String, List<String>> {
        logger.info("v3 getUserResourcesByPermissions user[$userId] scopeType[$scopeType] scopeId[$scopeId] resourceType[$resourceType] systemId[$systemId] permission[$permissions] supplier[$supplier]")
        val permissionMap = mutableMapOf<String, List<String>>()
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

    override fun validateUserResourcePermission(user: String, serviceCode: AuthServiceCode, resourceType: String, projectCode: String, permission: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(user: String, serviceCode: AuthServiceCode, resourceType: String, projectCode: String, resourceCode: String, permission: String, relationResourceType: String?): Boolean {
        logger.info("v3 validateUserResourcePermission user[$user] serviceCode[${serviceCode.id()}] resourceType[${resourceType}] permission[${permission}]")
        if (isProjectOwner(projectCode, user)) {
            return true
        }

        val actionType = buildAction(resourceType, permission)
        val instanceDTO = InstanceDTO()
        instanceDTO.system = serviceCode.id()
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

        logger.info("v3 validateUserResourcePermission instanceDTO[$instanceDTO]")
        return authHelper.isAllowed(user, actionType, instanceDTO)
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

    private fun buildAction(authResourceType: String, permission: String): String {
        return if (permission == AuthPermission.LIST.value) {
            "${authResourceType}_${AuthPermission.VIEW.value}"
        } else {
            "${authResourceType}_${permission}"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}