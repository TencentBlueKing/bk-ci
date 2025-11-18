/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.service.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import org.springframework.stereotype.Service

@Service
class RbacWorkspacePermissionService(
    private val client: Client,
    private val tokenService: ClientTokenService
) : WorkspacePermissionService {

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceName: String?,
        message: String
    ) {
        if (!hasPermission(userId, projectId, authPermission, workspaceName)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterWorkspace(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val action = buildAction(authPermission)
        return client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS,
            action = action
        ).data ?: emptyList()
    }

    override fun filterWorkspaces(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val actions = authPermissions.map { buildAction(it) }
        val permissionResourcesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            action = actions,
            resourceType = ResourceTypeId.CGS
        ).data ?: emptyMap()
        return permissionResourcesMap
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceName: String?
    ): Boolean {
        val action = buildAction(authPermission)
        val (resourceCode, resourceType) = if (workspaceName.isNullOrBlank()) {
            Pair(projectId, ResourceTypeId.PROJECT)
        } else {
            Pair(workspaceName, ResourceTypeId.CGS)
        }
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            relationResourceType = null,
            action = action,
            resourceCode = resourceCode
        ).data ?: false
    }

    override fun createResource(userId: String, projectId: String, workspaceName: String, displayName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS,
            resourceCode = workspaceName,
            resourceName = workspaceName
        )
    }

    override fun editResource(projectId: String, workspaceName: String, displayName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS,
            resourceCode = workspaceName,
            resourceName = workspaceName
        )
    }

    override fun deleteResource(projectId: String, workspaceName: String) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS,
            resourceCode = workspaceName
        )
    }

    private fun buildAction(permission: AuthPermission): String =
        "${ResourceTypeId.CGS.lowercase()}_${permission.value.lowercase()}"
}
