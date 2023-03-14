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
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.SubjectDTO
import com.tencent.bk.sdk.iam.dto.V2QueryPolicyDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO
import com.tencent.bk.sdk.iam.dto.resource.V2ResourceNode
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import org.slf4j.LoggerFactory

class RbacPermissionService constructor(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamConfiguration: IamConfiguration,
    private val policyService: PolicyService,
    private val authResourceCodeConverter: AuthResourceCodeConverter
) : PermissionService {
    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionService::class.java)
        private const val PATH_ATTRIBUTE = "_bk_iam_path_"
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
        val subject = SubjectDTO.builder()
            .id(userId)
            .type(ManagerScopesEnum.getType(ManagerScopesEnum.USER))
            .build()

        val actionDTO = ActionDTO()
        actionDTO.id = action

        val resourcePath = PathInfoDTO()
        resourcePath.system = iamConfiguration.systemId
        resourcePath.type = resourceType
        resourcePath.id = authResourceCodeConverter.code2IamCode(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val projectPath = PathInfoDTO()
        projectPath.system = iamConfiguration.systemId
        projectPath.type = AuthResourceType.PROJECT.value
        projectPath.id = projectCode
        projectPath.child = resourcePath

        val attribute = mapOf(
            PATH_ATTRIBUTE to listOf(projectPath.toString())
        )

        val resourceNode = V2ResourceNode.builder().system(iamConfiguration.systemId)
            .type(resourceType)
            .id(resourceCode)
            .attribute(attribute)
            .build()

        val queryPolicyDTO = V2QueryPolicyDTO.builder().system(iamConfiguration.systemId)
            .subject(subject)
            .action(actionDTO)
            .resources(listOf(resourceNode))
            .build()

        logger.info("[rbac] validateUserResourcePermission : queryPolicyDTO = $queryPolicyDTO")
        return policyService.verifyPermissions(queryPolicyDTO)
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        val actionList = actions.map { action ->
            val actionDTO = ActionDTO()
            actionDTO.id = action
            actionDTO
        }
        val resource = ResourceDTO.builder()
            .id(resourceCode)
            .type(resourceType)
            .system(iamConfiguration.systemId)
            .build()
        return policyService.batchVerifyPermissions(
            userId,
            actionList,
            listOf(resource)
        )
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        val subject = SubjectDTO.builder()
            .id(userId)
            .type(ManagerScopesEnum.getType(ManagerScopesEnum.USER))
            .build()

        val actionDTO = ActionDTO()
        actionDTO.id = action
        val paths = mutableListOf<PathInfoDTO>()
        resourcesPaths(
            projectCode = projectCode,
            resource = resource,
            child = null,
            paths = paths,
            needSystem = true
        )
        val attribute = mapOf(
            PATH_ATTRIBUTE to paths.map { it.toString() }
        )

        val resourceNode = V2ResourceNode.builder().system(iamConfiguration.systemId)
            .type(resource.resourceType)
            .id(
                authResourceCodeConverter.code2IamCode(
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
            )
            .attribute(attribute)
            .build()

        val queryPolicyDTO = V2QueryPolicyDTO.builder().system(iamConfiguration.systemId)
            .subject(subject)
            .action(actionDTO)
            .resources(listOf(resourceNode))
            .build()

        logger.info("[rbac] validateUserResourcePermission : queryPolicyDTO = $queryPolicyDTO")
        return policyService.verifyPermissions(queryPolicyDTO)
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
        val instanceMap = authHelper.groupRbacInstanceByType(userId, action)
        return when {
            resourceType == AuthResourceType.PROJECT.value ->
                instanceMap[resourceType] ?: emptyList()
            // 如果有项目下所有该资源权限,返回资源列表
            instanceMap[AuthResourceType.PROJECT.value]?.contains(projectCode) == true ->
                authResourceService.listByProjectAndType(
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            // 返回具体资源列表
            else ->
                instanceMap[resourceType]?.let {
                    authResourceCodeConverter.batchIamCode2Code(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        iamResourceCodes = it
                    )
                } ?: emptyList()
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

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        logger.info(
            "[rbac] getUserResourcesByActions : userId = $userId | actions = $action |" +
                " projectCode = $projectCode | resourceType = $resourceType"
        )
        return authHelper.groupRbacInstanceByType(userId, action)
    }

    private fun resourcesPaths(
        projectCode: String,
        resource: AuthResourceInstance,
        child: PathInfoDTO?,
        paths: MutableList<PathInfoDTO>,
        needSystem: Boolean
    ) {
        if (resource.parents.isNullOrEmpty()) {
            // 如果没有父资源,说明已经是最顶层
            if (child != null) {
                paths.add(child)
            }
        } else {
            resource.parents!!.forEach { parent ->
                val path = PathInfoDTO()
                if (needSystem) {
                    path.system = iamConfiguration.systemId
                }
                path.id = authResourceCodeConverter.code2IamCode(
                    projectCode = projectCode,
                    resourceType = parent.resourceType,
                    resourceCode = parent.resourceCode
                )
                path.type = parent.resourceType
                path.child = child
                resourcesPaths(
                    projectCode = projectCode,
                    resource = parent,
                    child = path,
                    paths = paths,
                    needSystem = needSystem
                )
            }
        }
    }
}
