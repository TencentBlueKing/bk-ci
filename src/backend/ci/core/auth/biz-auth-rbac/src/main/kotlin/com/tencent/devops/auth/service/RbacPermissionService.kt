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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import org.slf4j.LoggerFactory

class RbacPermissionService constructor(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamConfiguration: IamConfiguration,
    private val authResourceCodeConverter: AuthResourceCodeConverter
) : PermissionService {
    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionService::class.java)
    }

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        logger.info("[rbac] validateUserActionPermission :  userId = $userId | action = $action")
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

        logger.info("[rbac] validateUserResourcePermission : instanceDTO = $instanceDTO")
        return authHelper.isAllowed(userId, action, instanceDTO)
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        val instanceDTO = resource2InstanceDTO(resource = resource)
        logger.info("[rbac] validateUserResourcePermissionByInstance : instanceDTO = $instanceDTO")
        return authHelper.isAllowed(userId, action, instanceDTO)
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        logger.info(
            "[rbac] getUserResourcesByActions : userId = $userId | actions = $action |" +
                " projectCode = $projectCode | resourceType = $resourceType"
        )
        val instanceList = if (resourceType == AuthResourceType.PROJECT.value) {
            authHelper.getInstanceList(userId, action, resourceType)
        } else {
            val pathInfoDTO = PathInfoDTO()
            pathInfoDTO.type = AuthResourceType.PROJECT.value
            pathInfoDTO.id = projectCode
            authHelper.getInstanceList(userId, action, resourceType, pathInfoDTO)
        }
        return if (instanceList.contains("*")) {
            authResourceService.listByProjectAndType(
                projectCode = projectCode,
                resourceType = resourceType
            )
        } else {
            authResourceCodeConverter.batchIamCode2Code(
                projectCode = projectCode,
                resourceType = resourceType,
                iamResourceCodes = instanceList
            )
        }
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        logger.info(
            "[rbac] getUserResourcesByActions : userId = $userId | actions = $actions |" +
                " projectCode = $projectCode | resourceType = $resourceType"
        )
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

    override fun filterUserResourceByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resources: List<AuthResourceInstance>
    ): List<String> {
        logger.info("filter user resource by permission|$userId|$action|$projectCode")
        val instanceDTOList = resources.map { resource ->
            resource2InstanceDTO(resource)
        }
        return authHelper.isAllowed(userId, action, instanceDTOList)
    }

    private fun resource2InstanceDTO(resource: AuthResourceInstance): InstanceDTO {
        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        instanceDTO.id = resource.resourceCode
        instanceDTO.type = resource.resourceType
        if (!resource.parents.isNullOrEmpty()) {
            instanceDTO.paths = resource.parents!!.map {
                val path = PathInfoDTO()
                path.type = it.resourceType
                path.id = it.resourceCode
                path
            }.reversed()
        }
        return instanceDTO
    }
}
