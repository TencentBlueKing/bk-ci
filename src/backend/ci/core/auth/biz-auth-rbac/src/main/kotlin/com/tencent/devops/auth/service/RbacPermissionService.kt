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
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.SubjectDTO
import com.tencent.bk.sdk.iam.dto.V2QueryPolicyDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO
import com.tencent.bk.sdk.iam.dto.resource.V2ResourceNode
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.api.user.UserPipelineViewResource
import org.slf4j.LoggerFactory
import org.slf4j.MDC

@Suppress("TooManyFunctions", "LongMethod", "LongParameterList")
class RbacPermissionService constructor(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamConfiguration: IamConfiguration,
    private val policyService: PolicyService,
    private val authResourceCodeConverter: AuthResourceCodeConverter,
    private val permissionSuperManagerService: PermissionSuperManagerService,
    private val rbacCacheService: RbacCacheService,
    private val client: Client
) : PermissionService {
    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionService::class.java)
        private const val PATH_ATTRIBUTE = "_bk_iam_path_"
    }

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        logger.info("[rbac] validateUserActionPermission :  userId = $userId | action = $action")
        val startEpoch = System.currentTimeMillis()
        try {
            return authHelper.isAllowed(userId, action)
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to validate user action permission|" +
                    "$userId|$action"
            )
        }
    }

    /**
     * 如果没有具体资源,则校验是否有项目下任意资源权限
     */
    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        val actionInfo = rbacCacheService.getActionInfo(action)
        // 如果action关联的资源是项目,则直接查询项目的权限
        return if (actionInfo.relatedResourceType == AuthResourceType.PROJECT.value) {
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                relationResourceType = null
            )
        } else {
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = resourceType!!,
                resourceCode = "*",
                relationResourceType = null
            )
        }
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        val resource = buildAuthResourceInstance(
            userId = userId,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType
        )
        return validateUserResourcePermissionByInstance(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resource = resource
        )
    }

    @Suppress("ReturnCount", "ComplexMethod")
    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        logger.info(
            "[rbac] batch validate user resource permission|" +
                "$userId|$action|$projectCode|${resource.resourceType}|${resource.resourceCode}"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            // action需要兼容repo只传AuthPermission的情况,需要组装为Rbac的action
            val useAction = if (!action.contains("_")) {
                RbacAuthUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resource.resourceType))
            } else {
                action
            }
            if (isManager(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    action = useAction
                )
            ) {
                return true
            }
            val iamResourceCode = if (resource.resourceCode == "*") {
                resource.resourceCode
            } else {
                authResourceCodeConverter.code2IamCode(
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
            } ?: return false
            val subject = SubjectDTO.builder()
                .id(userId)
                .type(ManagerScopesEnum.getType(ManagerScopesEnum.USER))
                .build()

            val actionDTO = ActionDTO()
            actionDTO.id = useAction
            val paths = mutableListOf<PathInfoDTO>()
            resourcesPaths(
                projectCode = projectCode,
                resource = resource,
                child = null,
                paths = paths,
                needSystemFiled = true
            )
            val attribute = if (paths.isNotEmpty()) {
                mapOf(
                    PATH_ATTRIBUTE to paths.map { it.toString() }
                )
            } else {
                emptyMap()
            }

            val resourceNode = V2ResourceNode.builder().system(iamConfiguration.systemId)
                .type(resource.resourceType)
                .id(iamResourceCode)
                .attribute(attribute)
                .build()

            val queryPolicyDTO = V2QueryPolicyDTO.builder().system(iamConfiguration.systemId)
                .subject(subject)
                .action(actionDTO)
                .resources(listOf(resourceNode))
                .build()

            return policyService.verifyPermissions(queryPolicyDTO)
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to validate user resource permission|" +
                    "$userId|$action|$projectCode|${resource.resourceType}|${resource.resourceCode}"
            )
        }
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        val resource = buildAuthResourceInstance(
            userId = userId,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType
        )

        return batchValidateUserResourcePermissionByInstance(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resource = resource
        )
    }

    override fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean> {
        logger.info(
            "[rbac] batch validate user resource permission|" +
                "$userId|$actions|$projectCode|${resource.resourceType}|${resource.resourceCode}"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            if (rbacCacheService.checkProjectManager(userId = userId, projectCode = projectCode)) {
                return actions.associateWith { true }
            }
            val actionList = actions.map { action ->
                val actionDTO = ActionDTO()
                actionDTO.id = action
                actionDTO
            }
            val paths = mutableListOf<PathInfoDTO>()
            resourcesPaths(
                projectCode = projectCode,
                resource = resource,
                child = null,
                paths = paths,
                needSystemFiled = true
            )
            val attribute = if (paths.isNotEmpty()) {
                mapOf(
                    PATH_ATTRIBUTE to paths.map { it.toString() }
                )
            } else {
                emptyMap()
            }
            val iamResourceCode = authResourceCodeConverter.code2IamCode(
                projectCode = projectCode,
                resourceType = resource.resourceType,
                resourceCode = resource.resourceCode
            )
            val resourceDTO = ResourceDTO.builder()
                .id(iamResourceCode)
                .type(resource.resourceType)
                .attribute(attribute)
                .system(iamConfiguration.systemId)
                .build()
            return policyService.batchVerifyPermissions(
                userId,
                actionList,
                listOf(resourceDTO)
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to batch validate user resource permission|" +
                    "$userId|$actions|$projectCode|${resource.resourceType}|${resource.resourceCode}"
            )
        }
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        logger.info(
            "[rbac] get user resources|$userId|$action|$projectCode|$resourceType"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            // 拥有超级管理员权限,返回所有数据
            if (isManager(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
            ) {
                return authResourceService.listByProjectAndType(
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            }
            // action需要兼容repo只传AuthPermission的情况,需要组装为Rbac的action
            val useAction = if (!action.contains("_")) {
                RbacAuthUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resourceType))
            } else {
                action
            }
            val instanceMap = authHelper.groupRbacInstanceByType(userId, useAction)
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
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to get user resources|" +
                    "$userId|$action|$projectCode|$resourceType"
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
            "[rbac] batch get user resources|$userId|$actions|$projectCode|$resourceType"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            return actions.associate {
                val actionResourceList = getUserResourceByAction(
                    userId = userId,
                    action = it,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
                val authPermission = it.substringAfterLast("_")
                AuthPermission.get(authPermission) to actionResourceList
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to batch get user resources|" +
                    "$userId|$actions|$projectCode|$resourceType"
            )
        }
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        logger.info(
            "[rbac] batch get user resources and parent resource|$userId|$action|$projectCode|$resourceType"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            // 如果拥有超管权限,则拥有项目下所有数据
            if (isManager(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
            ) {
                return mapOf(AuthResourceType.PROJECT.value to listOf(projectCode))
            }
            return authHelper.groupRbacInstanceByType(userId, action).mapValues {
                authResourceCodeConverter.batchIamCode2Code(
                    projectCode = projectCode,
                    resourceType = it.key,
                    iamResourceCodes = it.value
                )
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to get user resources and parent resource|" +
                    "$userId|$action|$projectCode|$resourceType"
            )
        }
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        logger.info(
            "[rbac] filter user resources|$userId|$actions|$projectCode|$resourceType"
        )
        val startEpoch = System.currentTimeMillis()
        try {
            if (rbacCacheService.checkProjectManager(userId = userId, projectCode = projectCode)) {
                return actions.associate {
                    val authPermission = it.substringAfterLast("_")
                    AuthPermission.get(authPermission) to resources.map { resource -> resource.resourceCode }
                }
            }
            val instanceList = resources.map { resource ->
                val paths = mutableListOf<PathInfoDTO>()
                resourcesPaths(
                    projectCode = projectCode,
                    resource = resource,
                    child = null,
                    paths = paths,
                    needSystemFiled = false
                )
                val instance = InstanceDTO()
                instance.type = resource.resourceType
                instance.id = authResourceCodeConverter.code2IamCode(
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
                instance.system = iamConfiguration.systemId
                instance.paths = paths
                instance
            }
            val permissionMap = mutableMapOf<AuthPermission, List<String>>()
            val traceId = MDC.get(TraceTag.BIZID)
            actions.parallelStream().forEach { action ->
                MDC.put(TraceTag.BIZID, traceId)
                val authPermission = action.substringAfterLast("_")
                // 具有action管理员权限,那么有所有资源权限
                if (permissionSuperManagerService.reviewManagerCheck(
                        userId = userId,
                        projectCode = projectCode,
                        resourceType = resourceType,
                        action = action
                    )
                ) {
                    permissionMap[AuthPermission.get(authPermission)] = resources.map { it.resourceCode }
                } else {
                    val iamResourceCodes = authHelper.isAllowed(userId, action, instanceList)
                    permissionMap[AuthPermission.get(authPermission)] = authResourceCodeConverter.batchIamCode2Code(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        iamResourceCodes = iamResourceCodes
                    )
                }
            }
            return permissionMap
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to filter user resources |" +
                    "$userId|$actions|$projectCode|$resourceType"
            )
        }
    }

    private fun buildAuthResourceInstance(
        userId: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): AuthResourceInstance {
        val projectResourceInstance = AuthResourceInstance(
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
        return when (resourceType) {
            AuthResourceType.PROJECT.value ->
                projectResourceInstance
            // 流水线鉴权,需要添加关联的流水线组
            AuthResourceType.PIPELINE_DEFAULT.value -> {
                val parents = mutableListOf<AuthResourceInstance>()
                parents.add(projectResourceInstance)
                client.get(UserPipelineViewResource::class).listViewIdsByPipelineId(
                    userId = userId,
                    projectId = projectCode,
                    pipelineId = resourceCode
                ).data?.forEach { viewId ->
                    parents.add(
                        AuthResourceInstance(
                            resourceType = AuthResourceType.PIPELINE_GROUP.value,
                            resourceCode = HashUtil.encodeLongId(viewId),
                            parents = listOf(projectResourceInstance)
                        )
                    )
                }
                AuthResourceInstance(
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    parents = parents
                )
            }
            else -> {
                AuthResourceInstance(
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    parents = listOf(projectResourceInstance)
                )
            }
        }
    }

    /**
     * 将resource转换成iam path
     *
     * @param needSystemFiled path中是否需要系统字段,直接鉴权接口需要system字段，查询策略接口不需要
     */
    private fun resourcesPaths(
        projectCode: String,
        resource: AuthResourceInstance,
        child: PathInfoDTO?,
        paths: MutableList<PathInfoDTO>,
        needSystemFiled: Boolean
    ) {
        if (resource.parents.isNullOrEmpty()) {
            // 如果没有父资源,说明已经是最顶层
            if (child != null) {
                paths.add(child)
            }
        } else {
            resource.parents!!.forEach { parent ->
                val path = PathInfoDTO()
                if (needSystemFiled) {
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
                    needSystemFiled = needSystemFiled
                )
            }
        }
    }

    /**
     * 判断是否是管理员
     */
    private fun isManager(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): Boolean {
        return rbacCacheService.checkProjectManager(
            userId = userId,
            projectCode = projectCode
        ) || permissionSuperManagerService.reviewManagerCheck(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            action = action
        )
    }
}
